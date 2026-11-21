package pe.edu.esan.sportpro.ui.viewmodel

import com.google.firebase.firestore.FirebaseFirestoreException
import pe.edu.esan.sportpro.data.model.*

enum class SubmissionPhase { IDLE, SAVING, QUEUED, CONFIRMED, REJECTED, UNCERTAIN }
data class SubmissionProgress(val id: Long = 0, val phase: SubmissionPhase = SubmissionPhase.IDLE, val message: String? = null) {
    val blocksSend: Boolean get() = phase in listOf(SubmissionPhase.SAVING, SubmissionPhase.QUEUED, SubmissionPhase.UNCERTAIN)
}
internal fun firestoreFailurePhase(codeName: String): SubmissionPhase =
    if(codeName in setOf("PERMISSION_DENIED","UNAUTHENTICATED","INVALID_ARGUMENT","FAILED_PRECONDITION",
        "NOT_FOUND","ALREADY_EXISTS","OUT_OF_RANGE","ABORTED")) SubmissionPhase.REJECTED else SubmissionPhase.UNCERTAIN
internal fun writeFailure(error: Exception): SubmissionPhase = when (error) {
    is IllegalArgumentException -> SubmissionPhase.REJECTED
    is FirebaseFirestoreException -> firestoreFailurePhase(error.code.name)
    else -> SubmissionPhase.UNCERTAIN
}
internal fun uncertainWriteMessage() = "Sin confirmación: puede haberse guardado. Conservamos lo escrito; comprueba al reconectar y no lo reenvíes."

data class TextSubmission(val text: String = "", val postId: String? = null, val progress: SubmissionProgress = SubmissionProgress())
enum class CommunityWriteKind { POST, EDIT, COMMENT }
data class CommunityWriteRequest(val kind: CommunityWriteKind, val text: String, val postId: String? = null)
fun interface CommunityWriter { suspend fun write(request: CommunityWriteRequest) }

data class EventDraft(val old: MatchEvent?, val type: EventType, val minute: String, val team: String,
    val player: String, val note: String, val voided: Boolean = false)
data class EventSubmission(val key: String, val draft: EventDraft, val progress: SubmissionProgress,
    val eventId: String? = null) {
    val acceptedForEditor: Boolean get() = progress.phase==SubmissionPhase.QUEUED || progress.phase==SubmissionPhase.CONFIRMED
}
fun interface EventWriter { suspend fun write(draft: EventDraft, onQueued: (String) -> Unit) }
