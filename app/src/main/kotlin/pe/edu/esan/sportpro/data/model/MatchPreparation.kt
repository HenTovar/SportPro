package pe.edu.esan.sportpro.data.model

enum class Availability { PENDING, CONFIRMED, DECLINED }

data class MatchInvitation(
    val playerId: String,
    val name: String,
    val invited: Boolean = true,
    val response: Availability = Availability.PENDING,
    val respondedBy: String = "",
    val recordedByCoach: Boolean = false
)

data class MatchPreparation(
    val teamId: String = "",
    val revision: Long = 0,
    val lineupRevision: Long = -1,
    val roster: Map<String, MatchInvitation> = emptyMap(),
    val starters: List<String> = emptyList(),
    val substitutes: List<String> = emptyList(),
    val positions: Map<String, String> = emptyMap()
) {
    val lineupCurrent get() = lineupRevision == revision && starters.isNotEmpty()
    val hasLineup get() = lineupRevision >= 0
}

object MatchPreparationPolicy {
    val positions = listOf("PORTERO", "DEFENSA_CENTRAL", "LATERAL_IZQUIERDO",
        "LATERAL_DERECHO", "CENTROCAMPISTA", "EXTREMO", "DELANTERO")

    fun validId(id: String) = id.isNotBlank() && id.length <= 128 && '/' !in id

    fun checkRevision(actual: Long, expected: Long) {
        require(actual == expected) { "La convocatoria cambió. Revisa la información actual antes de guardar." }
    }

    fun validateInvitation(playerId: String, name: String, existingCount: Int, alreadyExists: Boolean) {
        require(validId(playerId)) { "Jugador no válido" }
        require(name.isNotBlank() && name.length <= 120) { "Nombre de jugador no válido" }
        require(alreadyExists || existingCount < 40) { "Máximo 40 jugadores en la convocatoria" }
    }

    fun validateResponse(invitation: MatchInvitation?, response: Availability) {
        require(invitation?.invited == true) { "La convocatoria ya no está vigente para esta ficha" }
        require(response != Availability.PENDING) { "Elige Confirmo o No puedo" }
    }

    fun canRecordResponse(user: User, teamId: String, playerId: String, recordedByCoach: Boolean): Boolean =
        user.uid.isNotBlank() && user.isActive && validId(teamId) && validId(playerId) &&
            if (recordedByCoach) RolePolicy.isStaff(user.role) && RolePolicy.canReadTeam(user, teamId)
            else FamilyPolicy.children(user).any { it.teamId == teamId && it.playerId == playerId }

    fun responseAttribution(invitation: MatchInvitation): String? =
        if (invitation.recordedByCoach && invitation.respondedBy.isNotBlank() &&
            invitation.response != Availability.PENDING) "Registrado por el entrenador" else null

    fun validateLineup(preparation: MatchPreparation, starters: List<String>, substitutes: List<String>, selectedPositions: Map<String, String>) {
        require(starters.isNotEmpty() && starters.size <= 11) { "Elige entre 1 y 11 titulares" }
        require(substitutes.size <= 12) { "Máximo 12 suplentes" }
        val all = starters + substitutes
        require(all.distinct().size == all.size) { "Un jugador no puede ocupar dos lugares" }
        require(selectedPositions.keys == all.toSet()) { "Asigna una posición a cada jugador seleccionado" }
        require(all.all { validId(it) && preparation.roster[it]?.let { row -> row.playerId == it && row.invited && row.response == Availability.CONFIRMED } == true }) {
            "Sólo puedes alinear jugadores convocados que confirmaron"
        }
        require(selectedPositions.values.all { it in positions }) { "Selecciona una posición válida" }
    }
}
