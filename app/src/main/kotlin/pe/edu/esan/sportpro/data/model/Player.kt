package pe.edu.esan.sportpro.data.model

/**
 * Modelo de datos para un jugador de un equipo.
 * Contiene información personal, física y de contacto.
 */
data class Player(
    val id: String = "",
    val teamId: String = "",
    val name: String = "",
    val position: PlayerPosition = PlayerPosition.DELANTERO,
    val number: Int = 0,
    val dateOfBirth: Long = 0,
    val height: Float = 0f,
    val weight: Float = 0f,
    val photoUrl: String? = null,
    val email: String = "",
    val phone: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val parentUid: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isActive: Boolean = true
)

/**
 * Posiciones en un equipo de fútbol.
 */
enum class PlayerPosition {
    PORTERO,
    DEFENSA_CENTRAL,
    LATERAL_IZQUIERDO,
    LATERAL_DERECHO,
    CENTROCAMPISTA,
    EXTREMO,
    DELANTERO
}

/**
 * Registro de asistencia a entrenamientos.
 */
data class AttendanceRecord(
    val id: String = "",
    val playerId: String = "",
    val trainingId: String = "",
    val status: AttendanceStatus = AttendanceStatus.ASISTIO,
    val notes: String = "",
    val recordedAt: Long = 0
)

/**
 * Estados posibles de asistencia.
 */
enum class AttendanceStatus {
    ASISTIO,
    FALTA,
    LESION,
    EXCUSED
}

/**
 * Estadísticas acumuladas de un jugador.
 */
data class PlayerStats(
    val playerId: String = "",
    val totalMatches: Int = 0,
    val totalGoals: Int = 0,
    val totalAssists: Int = 0,
    val yellowCards: Int = 0,
    val redCards: Int = 0,
    val attendanceRate: Float = 0f
)
