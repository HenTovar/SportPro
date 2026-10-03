package pe.edu.esan.sportpro.data.model

object RolePolicy {
    val registrationRoles = listOf(UserRole.ENTRENADOR, UserRole.JUGADOR, UserRole.PADRE)
    fun isStaff(role: UserRole?) = role == UserRole.ADMIN || role == UserRole.ENTRENADOR
    fun canReadTeam(user: User?, teamId: String) = user != null && teamId.isNotBlank() &&
        (isStaff(user.role) || user.teamId == teamId)
    fun canReadPlayer(user: User?, teamId: String, playerId: String) =
        canReadTeam(user, teamId) && playerId.isNotBlank() &&
            (isStaff(user?.role) || user?.playerId == playerId)
    /** Creator must be derived from verified Firebase Auth claims, never profile fields. */
    fun canApprove(approver: UserRole?, creator: Boolean, toRole: UserRole): Boolean =
        if (creator) true else when (toRole) {
            UserRole.ADMIN -> false
            UserRole.ENTRENADOR -> approver == UserRole.ADMIN
            UserRole.JUGADOR, UserRole.PADRE -> isStaff(approver)
        }
    fun label(role: UserRole?) = when (role) {
        UserRole.ADMIN -> "Administrador"
        UserRole.ENTRENADOR -> "Entrenador"
        UserRole.JUGADOR -> "Jugador"
        UserRole.PADRE -> "Padre / apoderado"
        null -> "Sin sesión"
    }
}
