package pe.edu.esan.sportpro.features

import pe.edu.esan.sportpro.data.model.*

/** UI choices only. Existing domain validation and server authorization are unchanged. */
object UiInteractionPolicy {
    fun positionLabel(value: String) = value.lowercase(java.util.Locale.ROOT).replace('_', ' ')
        .replaceFirstChar { it.uppercaseChar() }

    fun canStart(status: String) = status == "not_started"
    fun canFinish(status: String) = status == "in_progress"

    fun canChooseSlot(alreadySelected: Boolean, count: Int, limit: Int) =
        count >= 0 && limit > 0 && (alreadySelected || count < limit)

    fun hasAttendanceChoice(status: AttendanceStatus?, edited: Boolean) =
        edited || status == AttendanceStatus.ASISTIO || status == AttendanceStatus.FALTA

    /** Clicking the selected state is a no-op; an unregistered absence is still explicit. */
    fun attendanceChoice(currentPresent: Boolean, hasChoice: Boolean, desiredPresent: Boolean): Boolean? =
        if (hasChoice && currentPresent == desiredPresent) null else desiredPresent

    fun canReadExerciseLibrary(user: User?) = user != null && user.uid.isNotBlank() && user.isActive && RolePolicy.isStaff(user.role)

    fun exerciseCatalog(remote: List<Exercise>): Map<String, Exercise> =
        (ExerciseLibrary.defaultExercises + remote).distinctBy { it.id }.associateBy { it.id }

    fun exerciseLabel(id: String, catalog: Map<String, Exercise>): String = catalog[id]?.let {
        if (it.name.isBlank()) "Ejercicio no disponible" else "${it.name} · ${it.duration} min"
    } ?: "Ejercicio no disponible"

    fun categoryLabel(category: TeamCategory): String = when (category) {
        TeamCategory.SUB_10 -> "Sub-10"
        TeamCategory.SUB_12 -> "Sub-12"
        TeamCategory.SUB_15 -> "Sub-15"
        TeamCategory.SUB_17 -> "Sub-17"
        TeamCategory.PRIMERA -> "Primera"
        TeamCategory.SENIOR -> "Sénior"
    }
}
