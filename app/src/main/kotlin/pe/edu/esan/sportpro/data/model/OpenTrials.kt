package pe.edu.esan.sportpro.data.model

import java.text.Normalizer
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

enum class TrialState { PENDING, PUBLISHED, REJECTED }
enum class TrialPosition {
    ANY, PORTERO, DEFENSA_CENTRAL, LATERAL_IZQUIERDO, LATERAL_DERECHO,
    CENTROCAMPISTA, EXTREMO, DELANTERO;
    val label: String get() = if (this == ANY) "Cualquier posición" else name.replace('_', ' ')
}

data class OpenTrialDraft(
    val teamId: String,
    val title: String,
    val category: TeamCategory,
    val position: TrialPosition = TrialPosition.ANY,
    val dateMillis: Long,
    val location: String,
    val description: String
)

data class OpenTrial(
    val id: String,
    val draft: OpenTrialDraft,
    val state: TrialState = TrialState.PENDING,
    val createdBy: String = "",
    val createdAt: Long = 0,
    val updatedBy: String = "",
    val updatedAt: Long = 0,
    val approvedBy: String = "",
    val approvedAt: Long? = null,
    val revision: Long = 1
)

data class OpenTrialFilters(
    val category: TeamCategory? = null,
    val position: TrialPosition = TrialPosition.ANY,
    val from: String = "",
    val to: String = "",
    val location: String = ""
) {
    fun validate(): String? {
        val start = if (from.isBlank()) null else OpenTrialDates.parse(from)
        val end = if (to.isBlank()) null else OpenTrialDates.parse(to)
        return when {
            from.isNotBlank() && start == null -> "Fecha desde inválida. Usa AAAA-MM-DD."
            to.isNotBlank() && end == null -> "Fecha hasta inválida. Usa AAAA-MM-DD."
            start != null && end != null && start > end -> "La fecha desde debe ser anterior o igual a hasta."
            else -> null
        }
    }
}

/** Calendar in UTC keeps these dates usable on Android API 24 without desugaring. */
object OpenTrialDates {
    const val DAY_MILLIS = 86_400_000L
    fun parse(text: String): Long? {
        if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(text)) return null
        return try {
            val year = text.substring(0,4).toInt()
            if (year !in 1970..9999) return null
            val calendar = GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ROOT)
            calendar.isLenient = false
            calendar.clear()
            calendar.set(year, text.substring(5,7).toInt()-1, text.substring(8,10).toInt(),0,0,0)
            calendar.timeInMillis
        } catch (_: IllegalArgumentException) { null }
    }
    fun format(millis: Long): String {
        val calendar = GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ROOT)
        calendar.timeInMillis = millis
        return String.format(Locale.ROOT,"%04d-%02d-%02d",calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH)+1,calendar.get(Calendar.DAY_OF_MONTH))
    }
}

object OpenTrialsPolicy {
    private val contact = Regex("(@|https?://|www[.]|([+]?[0-9][ .()-]*){7,})", RegexOption.IGNORE_CASE)
    fun textError(value: String, max: Int): String? = when {
        value.isBlank() || value.length > max -> "Texto obligatorio, máximo $max caracteres."
        contact.containsMatchIn(value) -> "No incluyas teléfonos, correos ni enlaces de contacto."
        else -> null
    }
    fun validate(draft: OpenTrialDraft): String? = when {
        draft.teamId.isBlank() || '/' in draft.teamId -> "Selecciona un equipo."
        textError(draft.title,120) != null -> "Título: ${textError(draft.title,120)}"
        textError(draft.location,160) != null -> "Lugar: ${textError(draft.location,160)}"
        textError(draft.description,1500) != null -> "Descripción: ${textError(draft.description,1500)}"
        draft.dateMillis < 0 || draft.dateMillis % OpenTrialDates.DAY_MILLIS != 0L -> "Fecha inválida."
        else -> null
    }
    fun canPropose(user: User, teamId: String): Boolean = user.isActive && user.uid.isNotBlank() &&
        teamId.isNotBlank() && (user.role == UserRole.ADMIN ||
        (user.role == UserRole.ENTRENADOR && teamId in user.coachTeamIds))
    fun canModerate(user: User, trial: OpenTrial): Boolean = user.isActive &&
        user.uid.isNotBlank() && user.role == UserRole.ADMIN && trial.state == TrialState.PENDING
    fun canRevise(user: User, trial: OpenTrial): Boolean = canPropose(user, trial.draft.teamId)
    fun moderationTarget(user: User, trial: OpenTrial, expectedRevision: Long, publish: Boolean): TrialState {
        require(canModerate(user,trial)) { "Sólo un administrador puede moderar una propuesta pendiente." }
        require(trial.revision == expectedRevision) { "El aviso cambió. Recarga antes de continuar." }
        return if (publish) TrialState.PUBLISHED else TrialState.REJECTED
    }
    fun normalize(text: String): String = Normalizer.normalize(text,Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"),"").lowercase(Locale.ROOT).trim().replace(Regex("\\s+")," ")
    /** Apply to the entire authorized query result; never paginate before filtering. */
    fun filter(trials: List<OpenTrial>, filters: OpenTrialFilters): List<OpenTrial> {
        require(filters.validate() == null) { filters.validate().orEmpty() }
        val from = if (filters.from.isBlank()) null else OpenTrialDates.parse(filters.from)
        val to = if (filters.to.isBlank()) null else OpenTrialDates.parse(filters.to)
        val place = normalize(filters.location)
        return trials.filter { trial ->
            val d = trial.draft
            (filters.category == null || d.category == filters.category) &&
                (filters.position == TrialPosition.ANY || d.position == TrialPosition.ANY || d.position == filters.position) &&
                (from == null || d.dateMillis >= from) && (to == null || d.dateMillis <= to) &&
                (place.isEmpty() || normalize(d.location).contains(place))
        }.sortedWith(compareBy<OpenTrial> { it.draft.dateMillis }.thenBy { normalize(it.draft.title) }.thenBy { it.id })
    }
}
