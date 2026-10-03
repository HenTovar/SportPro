package pe.edu.esan.sportpro.data.model

/**
 * Modelo de datos para un equipo de fútbol.
 */
data class Team(
    val id: String = "",
    val name: String = "",
    val category: TeamCategory = TeamCategory.SUB_15,
    val description: String = "",
    val ownerId: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isActive: Boolean = true
)

/**
 * Categorías de equipos en SportPro.
 */
enum class TeamCategory {
    SUB_10,
    SUB_12,
    SUB_15,
    SUB_17,
    PRIMERA,
    SENIOR
}

/**
 * Información de jugadores registrados en un equipo.
 */
data class TeamStats(
    val totalPlayers: Int = 0,
    val totalTrainings: Int = 0,
    val totalMatches: Int = 0
)
