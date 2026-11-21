package pe.edu.esan.sportpro.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.Availability
import pe.edu.esan.sportpro.data.model.MatchInvitation
import pe.edu.esan.sportpro.data.model.MatchPreparation
import pe.edu.esan.sportpro.data.model.MatchPreparationPolicy

data class PreparationSnapshot(val preparation: MatchPreparation, val cached: Boolean, val pending: Boolean)

/** One transactional document per team; no profile, contact or credential fields. */
class MatchPreparationRepository(private val db: FirebaseFirestore, private val auth: FirebaseAuth) {
    private fun actor() = requireNotNull(auth.currentUser?.uid) { "Inicia sesión" }
    private fun doc(matchId: String, teamId: String) =
        db.collection("matches").document(matchId).collection("squads").document(teamId)

    private fun parse(s: DocumentSnapshot, teamId: String): MatchPreparation {
        if (!s.exists()) return MatchPreparation(teamId = teamId)
        require(s.getString("teamId") == teamId) { "La preparación no corresponde al equipo" }
        val roster = (s.get("roster") as? Map<*, *>)?.map { (id, raw) ->
            require(id is String && raw is Map<*, *>) { "Convocatoria con formato no válido" }
            val response = raw["response"] as? String ?: error("Falta el estado de convocatoria")
            id to MatchInvitation(id, raw["name"] as? String ?: error("Falta el nombre del jugador"),
                raw["invited"] as? Boolean ?: error("Falta el estado de invitación"),
                Availability.valueOf(response), raw["respondedBy"] as? String ?: "",
                raw["recordedByCoach"] as? Boolean ?: false)
        }?.toMap() ?: error("No se pudo leer la convocatoria")
        fun ids(key: String) = (s.get(key) as? List<*>)?.map {
            require(it is String) { "Alineación con formato no válido" }; it
        } ?: error("No se pudo leer la alineación")
        val positions = (s.get("positions") as? Map<*, *>)?.map { (id, p) ->
            require(id is String && p is String) { "Posición con formato no válido" }; id to p
        }?.toMap() ?: error("No se pudieron leer las posiciones")
        return MatchPreparation(teamId, s.getLong("revision") ?: error("Falta la revisión"),
            s.getLong("lineupRevision") ?: -1, roster, ids("starters"), ids("substitutes"), positions)
    }

    fun watch(matchId: String, teamId: String) = callbackFlow<Result<PreparationSnapshot>> {
        val listener = doc(matchId, teamId).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) trySend(Result.Error("No se pudo leer la preparación. Revisa tu acceso al equipo."))
            else if (snapshot != null) {
                try { trySend(Result.Success(PreparationSnapshot(parse(snapshot, teamId),
                    snapshot.metadata.isFromCache, snapshot.metadata.hasPendingWrites()))) }
                catch (_: IllegalArgumentException) { trySend(Result.Error("La preparación tiene datos no válidos. Pide al entrenador que la revise.")) }
                catch (_: IllegalStateException) { trySend(Result.Error("No se pudo leer la preparación guardada. Pide al entrenador que la revise.")) }
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun invite(matchId: String, teamId: String, playerId: String, name: String,
                       invited: Boolean, expectedRevision: Long) {
        val uid = actor()
        val ref = doc(matchId, teamId)
        db.runTransaction { tx ->
            val old = tx.get(ref)
            val state = parse(old, teamId)
            MatchPreparationPolicy.checkRevision(state.revision, expectedRevision)
            require(invited || playerId in state.roster) { "No existe una convocatoria para retirar" }
            val storedName = if (!invited) state.roster.getValue(playerId).name else name.trim()
            MatchPreparationPolicy.validateInvitation(playerId, storedName, state.roster.size, playerId in state.roster)
            val roster = (old.get("roster") as? Map<*, *>)?.entries?.associate { it.key as String to it.value }?.toMutableMap()
                ?: mutableMapOf<String, Any?>()
            roster[playerId] = mapOf("name" to storedName, "invited" to invited,
                "response" to Availability.PENDING.name, "respondedBy" to "", "respondedAt" to null,
                "recordedByCoach" to false)
            val changes = mapOf("roster" to roster, "lastPlayerId" to playerId,
                "revision" to state.revision + 1, "updatedBy" to uid, "updatedAt" to FieldValue.serverTimestamp())
            if (old.exists()) tx.update(ref, changes)
            else tx.set(ref, changes + mapOf("teamId" to teamId, "lineupRevision" to -1L,
                "starters" to emptyList<String>(), "substitutes" to emptyList<String>(), "positions" to emptyMap<String, String>()))
        }.await()
    }

    suspend fun respond(matchId: String, teamId: String, playerId: String,
                        response: Availability, expectedRevision: Long, recordedByCoach: Boolean = false) {
        val uid = actor()
        val ref = doc(matchId, teamId)
        db.runTransaction { tx ->
            val old = tx.get(ref)
            val state = parse(old, teamId)
            MatchPreparationPolicy.checkRevision(state.revision, expectedRevision)
            MatchPreparationPolicy.validateResponse(state.roster[playerId], response)
            val raw = old.get("roster") as? Map<*, *> ?: error("La convocatoria ya no está disponible")
            val roster = raw.entries.associate { it.key as String to it.value }.toMutableMap()
            val invitation = (raw[playerId] as? Map<*, *>)?.entries?.associate { it.key as String to it.value }
                ?: error("No se encontró tu convocatoria")
            roster[playerId] = invitation + mapOf("response" to response.name,
                "respondedBy" to uid, "respondedAt" to FieldValue.serverTimestamp(),
                "recordedByCoach" to recordedByCoach)
            tx.update(ref, mapOf("roster" to roster, "lastPlayerId" to playerId,
                "revision" to state.revision + 1, "updatedBy" to uid, "updatedAt" to FieldValue.serverTimestamp()))
        }.await()
    }

    suspend fun saveLineup(matchId: String, teamId: String, starters: List<String>, substitutes: List<String>,
                           positions: Map<String, String>, expectedRevision: Long) {
        val uid = actor()
        val ref = doc(matchId, teamId)
        db.runTransaction { tx ->
            val old = tx.get(ref)
            val state = parse(old, teamId)
            MatchPreparationPolicy.checkRevision(state.revision, expectedRevision)
            MatchPreparationPolicy.validateLineup(state, starters, substitutes, positions)
            tx.update(ref, mapOf("starters" to starters, "substitutes" to substitutes, "positions" to positions,
                "revision" to state.revision + 1, "lineupRevision" to state.revision + 1,
                "updatedBy" to uid, "updatedAt" to FieldValue.serverTimestamp()))
        }.await()
    }
}
