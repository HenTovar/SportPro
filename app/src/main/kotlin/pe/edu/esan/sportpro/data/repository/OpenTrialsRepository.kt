package pe.edu.esan.sportpro.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.*

class OpenTrialsRepository(private val db: FirebaseFirestore, private val auth: FirebaseAuth) {
    private val cached = MutableStateFlow(true)
    val isFromCache: StateFlow<Boolean> = cached.asStateFlow()
    private val collection get() = db.collection("openTrials")
    private fun uid(): String = requireNotNull(auth.currentUser?.uid) { "Inicia sesión." }
    fun watchPublished(): Flow<Result<List<OpenTrial>>> = watch(collection.whereEqualTo("state","PUBLISHED"))
    fun watchPending(): Flow<Result<List<OpenTrial>>> = watch(collection.whereEqualTo("state","PENDING"))
    fun watchTeam(teamId: String): Flow<Result<List<OpenTrial>>> {
        require(teamId.isNotBlank() && '/' !in teamId)
        return watch(collection.whereEqualTo("teamId",teamId))
    }
    private fun watch(query: Query): Flow<Result<List<OpenTrial>>> = callbackFlow {
        cached.value = true
        trySend(Result.Loading())
        // No limit: category, date, position and accent-insensitive location filter the full result.
        val registration = query.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            cached.value = snapshot == null || snapshot.metadata.isFromCache || snapshot.metadata.hasPendingWrites()
            if (error != null) trySend(Result.Error(userMessage(error)))
            else try {
                trySend(Result.Success(snapshot?.documents.orEmpty().map(::decode)))
            } catch (_: Exception) {
                trySend(Result.Error("Hay un aviso inválido. Solicita su revisión al administrador."))
            }
        }
        awaitClose { registration.remove() }
    }
    private fun decode(s: DocumentSnapshot): OpenTrial = OpenTrial(
        id=s.id,
        draft=OpenTrialDraft(s.getString("teamId").orEmpty(),s.getString("title").orEmpty(),
            TeamCategory.valueOf(requireNotNull(s.getString("category"))),
            TrialPosition.valueOf(requireNotNull(s.getString("position"))),
            requireNotNull(s.getLong("dateMillis")),s.getString("location").orEmpty(),s.getString("description").orEmpty()),
        state=TrialState.valueOf(requireNotNull(s.getString("state"))),
        createdBy=s.getString("createdBy").orEmpty(),createdAt=s.getTimestamp("createdAt")?.toDate()?.time ?: 0,
        updatedBy=s.getString("updatedBy").orEmpty(),updatedAt=s.getTimestamp("updatedAt")?.toDate()?.time ?: 0,
        approvedBy=s.getString("approvedBy").orEmpty(),approvedAt=s.getTimestamp("approvedAt")?.toDate()?.time,
        revision=requireNotNull(s.getLong("revision")))
    private fun fields(d: OpenTrialDraft): Map<String,Any> = mapOf("teamId" to d.teamId,
        "title" to d.title.trim(),"category" to d.category.name,"position" to d.position.name,
        "dateMillis" to d.dateMillis,"location" to d.location.trim(),"description" to d.description.trim())
    private fun requireActor(tx: Transaction, user: User, team: String? = null): String {
        val actor = uid()
        check(user.uid == actor && user.isActive) { "La sesión cambió. Vuelve a ingresar." }
        val account = tx.get(db.collection("users").document(actor))
        check((account.getBoolean("active") ?: account.getBoolean("isActive")) == true) { "Cuenta inactiva." }
        val role = account.getString("role")
        if (team == null) check(role == "ADMIN") { "Sólo un administrador puede moderar." }
        else {
            val target = tx.get(db.collection("teams").document(team))
            check((target.getBoolean("active") ?: target.getBoolean("isActive")) == true) { "Equipo inactivo." }
            val scoped = account.get("coachTeamIds")
            val owns = if (scoped is List<*>) team in scoped else
                account.getString("teamId") == team || target.getString("ownerId") == actor
            check(role == "ADMIN" || (role == "ENTRENADOR" && owns)) { "Requiere personal del equipo." }
        }
        return actor
    }
    private suspend fun <T> result(action: suspend () -> T): Result<T> = try { Result.Success(action()) }
    catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.Error(userMessage(e))
    }
    private fun userMessage(error: Exception): String = when(error) {
        is FirebaseFirestoreException -> when(error.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> "No tienes permiso o tu sesión cambió. Vuelve a ingresar; si continúa, consulta al administrador."
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> "No hay confirmación del servidor. Revisa tu conexión y recarga antes de volver a intentar."
            FirebaseFirestoreException.Code.ABORTED,
            FirebaseFirestoreException.Code.FAILED_PRECONDITION -> "El aviso cambió o no pudo confirmarse. Recarga antes de volver a intentar."
            FirebaseFirestoreException.Code.NOT_FOUND -> "El aviso ya no está disponible. Recarga la lista."
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> "El servicio está ocupado. Espera un momento y recarga antes de volver a intentar."
            else -> "No se pudo confirmar la operación. Recarga y vuelve a intentar."
        }
        is IllegalArgumentException, is IllegalStateException -> error.message ?: "Revisa los datos y vuelve a intentar."
        else -> "No se pudo confirmar la operación. Revisa la conexión y recarga antes de volver a intentar."
    }
    suspend fun propose(user: User, draft: OpenTrialDraft): Result<String> = result {
        require(OpenTrialsPolicy.canPropose(user,draft.teamId)) { "Requiere personal del equipo." }
        require(OpenTrialsPolicy.validate(draft) == null) { OpenTrialsPolicy.validate(draft).orEmpty() }
        val ref = collection.document()
        db.runTransaction { tx ->
            val actor = requireActor(tx,user,draft.teamId)
            tx.set(ref,fields(draft)+mapOf("state" to "PENDING","createdBy" to actor,
                "createdAt" to FieldValue.serverTimestamp(),"updatedBy" to actor,
                "updatedAt" to FieldValue.serverTimestamp(),"approvedBy" to "","approvedAt" to null,"revision" to 1L))
        }.await()
        ref.id
    }
    suspend fun revise(user: User, id: String, expectedRevision: Long, draft: OpenTrialDraft): Result<Unit> = result {
        require(id.isNotBlank() && '/' !in id && expectedRevision >= 1)
        require(OpenTrialsPolicy.canPropose(user,draft.teamId)) { "Requiere personal del equipo." }
        require(OpenTrialsPolicy.validate(draft) == null) { OpenTrialsPolicy.validate(draft).orEmpty() }
        val ref = collection.document(id)
        db.runTransaction { tx ->
            val actor = requireActor(tx,user,draft.teamId)
            val before = tx.get(ref)
            check(before.exists() && before.getLong("revision") == expectedRevision) { "El aviso cambió. Recarga antes de continuar." }
            check(before.getString("teamId") == draft.teamId) { "El equipo del aviso no puede cambiar." }
            tx.update(ref,fields(draft)+mapOf("state" to "PENDING","approvedBy" to "","approvedAt" to null,
                "updatedBy" to actor,"updatedAt" to FieldValue.serverTimestamp(),"revision" to expectedRevision+1))
        }.await()
        Unit
    }
    suspend fun moderate(user: User, id: String, expectedRevision: Long, publish: Boolean): Result<Unit> = result {
        require(id.isNotBlank() && '/' !in id && expectedRevision >= 1)
        val ref = collection.document(id)
        db.runTransaction { tx ->
            val actor = requireActor(tx,user)
            val before = tx.get(ref)
            check(before.exists()) { "Aviso no encontrado." }
            val target = OpenTrialsPolicy.moderationTarget(user,decode(before),expectedRevision,publish)
            tx.update(ref,mapOf("state" to target.name,"approvedBy" to if(publish) actor else "",
                "approvedAt" to if(publish) FieldValue.serverTimestamp() else null,
                "updatedBy" to actor,"updatedAt" to FieldValue.serverTimestamp(),"revision" to expectedRevision+1))
        }.await()
        Unit
    }
}
