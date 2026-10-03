package pe.edu.esan.sportpro.data.model

object RolePolicy {
    val registrationRoles = listOf(UserRole.ENTRENADOR, UserRole.JUGADOR, UserRole.PADRE)
    fun isStaff(role: UserRole?) = role == UserRole.ADMIN || role == UserRole.ENTRENADOR
    fun canReadTeam(user: User?, teamId: String) = user != null && teamId.isNotBlank() &&
        (isStaff(user.role) || user.teamId == teamId)
    fun canReadPlayer(user: User?, teamId: String, playerId: String) =
        canReadTeam(user, teamId) && playerId.isNotBlank() &&
            (isStaff(user?.role) || user?.playerId == playerId)
    fun label(role: UserRole?) = when (role) {
        UserRole.ADMIN -> "Administrador"
        UserRole.ENTRENADOR -> "Entrenador"
        UserRole.JUGADOR -> "Jugador"
        UserRole.PADRE -> "Padre / apoderado"
        null -> "Sin sesión"
    }
}
