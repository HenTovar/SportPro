package pe.edu.esan.sportpro.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.FamilyPolicy
import pe.edu.esan.sportpro.data.model.SimulatedFeeRecord
import pe.edu.esan.sportpro.data.model.SimulatedFeeStatus
import pe.edu.esan.sportpro.data.model.SimulatedFeesPolicy
import pe.edu.esan.sportpro.data.model.User

data class SimulatedFeePlayerChoice(val id: String, val name: String)

data class SimulatedFeeSnapshot(
    val record: SimulatedFeeRecord?,
    val cached: Boolean,
    val pending: Boolean
) {
    val confirmed: Boolean get() = !cached && !pending
    val revision: Long get() = record?.revision ?: 0L
}

/** Academic state tracking only. Every stored status remains explicitly simulated. */
class SimulatedFeesRepository(private val db: FirebaseFirestore, private val auth: FirebaseAuth) {
    private fun actor(user: User): String {
        val uid = auth.currentUser?.uid
        require(!uid.isNullOrBlank() && uid == user.uid && user.isActive) {
            "La sesión cambió. Vuelve a iniciar sesión."
        }
        return uid
    }

    private fun playerRef(teamId: String, playerId: String) =
        db.collection("teams").document(teamId).collection("players").document(playerId)

    private fun document(teamId: String, playerId: String, period: String) =
        playerRef(teamId, playerId).collection("simulatedFees").document(period)

    private fun parse(snapshot: DocumentSnapshot, teamId: String, playerId: String, period: String): SimulatedFeeRecord? {
        if (!snapshot.exists()) return null
        val data = requireNotNull(snapshot.data) { "El registro no se puede leer." }
        require(data.keys == SimulatedFeesPolicy.documentKeys && data["simulated"] == true) {
            "El registro no tiene el formato de simulación esperado."
        }
        require(data["teamId"] == teamId && data["playerId"] == playerId && data["period"] == period) {
            "El registro no corresponde a la ficha y el período seleccionados."
        }
        val revision = data["revision"] as? Long
        require(revision != null && revision >= 1) { "El registro tiene una revisión no válida." }
        val createdBy = data["createdBy"] as? String
        val updatedBy = data["updatedBy"] as? String
        require(!createdBy.isNullOrBlank() && !updatedBy.isNullOrBlank()) { "Falta el autor del registro." }
        val createdAt = data["createdAt"] as? Timestamp
        val updatedAt = data["updatedAt"] as? Timestamp
        require(snapshot.metadata.hasPendingWrites() || (createdAt != null && updatedAt != null)) {
            "Falta la confirmación de fecha del servidor."
        }
        return SimulatedFeeRecord(teamId, playerId, period,
            SimulatedFeesPolicy.parseStatus(data["status"] as? String ?: ""), revision,
            createdBy, createdAt?.toDate()?.time, updatedBy, updatedAt?.toDate()?.time)
    }

    /** Uses server reads; restricted accounts never query the entire team roster. */
    suspend fun loadPlayers(teamId: String, user: User): List<SimulatedFeePlayerChoice> {
        actor(user)
        require(SimulatedFeesPolicy.validId(teamId)) { "Selecciona un equipo válido." }
        val snapshots = if (SimulatedFeesPolicy.canManage(user)) {
            db.collection("teams").document(teamId).collection("players").get(Source.SERVER).await().documents
        } else {
            val children = FamilyPolicy.children(user).filter { it.teamId == teamId }
            require(children.isNotEmpty()) { "No tienes una ficha autorizada en este equipo." }
            children.map { child ->
                require(SimulatedFeesPolicy.canRead(user, teamId, child.playerId)) { "No tienes acceso a esta ficha." }
                playerRef(teamId, child.playerId).get(Source.SERVER).await()
            }
        }
        return snapshots.filter { it.exists() }.map { snapshot ->
            require(SimulatedFeesPolicy.validId(snapshot.id)) { "Se encontró una ficha con identificador no válido." }
            SimulatedFeePlayerChoice(snapshot.id,
                snapshot.getString("name")?.takeIf { it.isNotBlank() } ?: "Ficha ${snapshot.id.take(8)}")
        }.sortedBy { it.name.lowercase() }
    }

    fun watch(teamId: String, playerId: String, period: String, user: User): Flow<Result<SimulatedFeeSnapshot>> = callbackFlow {
        try {
            actor(user)
            SimulatedFeesPolicy.requirePath(teamId, playerId, period)
            require(SimulatedFeesPolicy.canRead(user, teamId, playerId)) { "No tienes acceso a esta ficha." }
        } catch (e: IllegalArgumentException) {
            trySend(Result.Error(errorMessage(e)))
            close()
            return@callbackFlow
        }
        trySend(Result.Loading())
        val listener = document(teamId, playerId, period).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            when {
                error != null -> trySend(Result.Error(errorMessage(error)))
                snapshot != null -> try {
                    trySend(Result.Success(SimulatedFeeSnapshot(parse(snapshot, teamId, playerId, period),
                        snapshot.metadata.isFromCache, snapshot.metadata.hasPendingWrites())))
                } catch (_: IllegalArgumentException) {
                    trySend(Result.Error("El registro guardado no es válido. Solicita revisión al administrador."))
                }
            }
        }
        awaitClose { listener.remove() }
    }

    /** Returns only after the transaction receives its server acknowledgement. */
    suspend fun save(teamId: String, playerId: String, period: String, status: SimulatedFeeStatus,
                     expectedRevision: Long, user: User): Long {
        val uid = actor(user)
        SimulatedFeesPolicy.requirePath(teamId, playerId, period)
        require(SimulatedFeesPolicy.canManage(user)) { "Sólo un administrador puede cambiar estados simulados." }
        val ref = document(teamId, playerId, period)
        return db.runTransaction { transaction ->
            val player = transaction.get(playerRef(teamId, playerId))
            require(player.exists()) { "La ficha ya no existe. Vuelve a cargar el equipo." }
            val old = transaction.get(ref)
            val record = parse(old, teamId, playerId, period)
            SimulatedFeesPolicy.requireRevision(record?.revision ?: 0, expectedRevision)
            val nextRevision = (record?.revision ?: 0) + 1
            val changes = mapOf("status" to status.name, "revision" to nextRevision,
                "updatedBy" to uid, "updatedAt" to FieldValue.serverTimestamp())
            if (old.exists()) transaction.update(ref, changes)
            else transaction.set(ref, changes + mapOf("teamId" to teamId, "playerId" to playerId,
                "period" to period, "simulated" to true, "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp()))
            nextRevision
        }.await()
    }

    fun errorMessage(error: Exception): String = when (error) {
        is FirebaseFirestoreException -> when (error.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> "No tienes permiso para consultar o cambiar este registro. Vuelve a cargar tu sesión."
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> "La sesión terminó. Vuelve a iniciar sesión."
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> "No hay confirmación del servidor. Comprueba la conexión y vuelve a cargar antes de reintentar."
            FirebaseFirestoreException.Code.ABORTED -> "El registro cambió. Vuelve a cargar antes de guardar."
            else -> "No se pudo confirmar la operación. Vuelve a cargar el registro antes de reintentar."
        }
        is IllegalArgumentException -> error.message ?: "Revisa la ficha y el período seleccionados."
        else -> "No se pudo confirmar la operación. Comprueba la conexión y vuelve a cargar el registro."
    }
}
