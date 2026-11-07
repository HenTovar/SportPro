package pe.edu.esan.sportpro.data.model

object RolePolicy {
    val registrationRoles = listOf(UserRole.JUGADOR, UserRole.PADRE)
    val registrationChoices = listOf(UserRole.ENTRENADOR, UserRole.JUGADOR, UserRole.PADRE)
    fun registrationBaseRole(choice: UserRole): UserRole {
        require(choice in registrationChoices) { "El administrador no se asigna en el registro" }
        return if (choice == UserRole.ENTRENADOR) UserRole.JUGADOR else choice
    }
    const val coachPendingNotice = "Solicitud enviada, pendiente de aprobación por un administrador. Tu cuenta conserva el rol Jugador y no tiene acceso de entrenador."
    fun isStaff(role: UserRole?) = role == UserRole.ADMIN || role == UserRole.ENTRENADOR
    fun canReadTeam(user: User?, teamId: String) = user != null && user.isActive && teamId.isNotBlank() &&
        (user.role == UserRole.ADMIN || (user.role == UserRole.ENTRENADOR && teamId in user.coachTeamIds) || (user.role in listOf(UserRole.JUGADOR, UserRole.PADRE) && !user.hasFamilyRevocation && user.teamId == teamId) || FamilyPolicy.children(user).any { it.teamId == teamId })
    fun canReadPlayer(user: User?, teamId: String, playerId: String) =
        canReadTeam(user, teamId) && playerId.isNotBlank() &&
            (isStaff(user?.role) || (user?.hasFamilyRevocation == false && user.teamId == teamId && user.playerId == playerId) ||
                FamilyPolicy.children(user).any { it.teamId == teamId && it.playerId == playerId })
    /** Creator must be derived from verified Firebase Auth claims, never profile fields. */
    fun canApprove(approver: UserRole?, creator: Boolean, toRole: UserRole): Boolean =
        if (toRole == UserRole.ENTRENADOR) approver == UserRole.ADMIN else if (creator) true else when (toRole) {
            UserRole.ADMIN -> false
            UserRole.ENTRENADOR -> approver == UserRole.ADMIN
            UserRole.JUGADOR, UserRole.PADRE -> isStaff(approver)
        }
    fun approvalFields(target: UserRole): Map<String, Any> =
        mapOf("role" to target.name, "children" to emptyMap<String, Any>(), "teamId" to "", "playerId" to "", "coachTeamIds" to emptyList<String>())
    fun label(role: UserRole?) = when (role) {
        UserRole.ADMIN -> "Administrador"
        UserRole.ENTRENADOR -> "Entrenador"
        UserRole.JUGADOR -> "Jugador"
        UserRole.PADRE -> "Padre / apoderado"
        null -> "Sin sesión"
    }
}
