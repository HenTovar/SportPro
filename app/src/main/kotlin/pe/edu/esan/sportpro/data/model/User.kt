package pe.edu.esan.sportpro.data.model

/**
 * Modelo de datos para un usuario de SportPro.
 * Contiene información del perfil y rol del usuario.
 */
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.JUGADOR,
    val photoUrl: String? = null,
    val createdAt: Long = 0,
    val isActive: Boolean = true
)

/**
 * Roles de usuario en SportPro:
 * - ADMIN: Administrador de la academia
 * - ENTRENADOR: Director técnico o entrenador
 * - JUGADOR: Jugador del equipo
 * - PADRE: Padre o apoderado
 */
enum class UserRole {
    ADMIN,
    ENTRENADOR,
    JUGADOR,
    PADRE
}

/**
 * Estado de autenticación del usuario.
 */
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}
