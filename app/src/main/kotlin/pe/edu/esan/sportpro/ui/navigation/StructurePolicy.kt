package pe.edu.esan.sportpro.ui.navigation

import java.net.URLEncoder
import java.text.Normalizer
import pe.edu.esan.sportpro.data.model.*

/** Presentation-only routing and filtering. Never grants a server permission. */
object StructurePolicy {
    fun selectionKey(uid: String) = uid.takeIf { it.isNotBlank() }?.let { "last_team_$it" }
    fun destinations(user: User?): List<String> =
        if (user?.isActive == true && (RolePolicy.isStaff(user.role) || user.teamId.isNotBlank()))
            listOf("Inicio", "Partidos", "Equipo", "Comunidad", "Perfil")
        else listOf("Inicio", "Perfil")
    fun selectedTeam(user: User?, remembered: String, teams: List<Team>): Team? {
        if (user?.isActive != true) return null
        val allowed = teams.filter { it.id.isNotBlank() && it.isActive && RolePolicy.canReadTeam(user, it.id) }
        return if (RolePolicy.isStaff(user.role)) allowed.find { it.id == remembered } ?: allowed.firstOrNull()
        else allowed.find { it.id == user.teamId }
    }
    fun component(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    fun team(id: String, tab: String = "players") = "team/${component(id)}?tab=${tab.takeIf { it in teamTabs } ?: "players"}"
    val teamTabs = listOf("players", "trainings", "matches", "community")
    fun match(id: String, event: Boolean = false) = "liveMatch/${component(id)}?event=$event"
    fun attendance(team: String, training: String) = "attendance/${component(team)}/${component(training)}"
    fun ownAttendance(user: User?): String? = user?.takeIf { it.isActive && it.teamId.isNotBlank() && it.playerId.isNotBlank() }
        ?.let { "attendanceHistory/${component(it.teamId)}/${component(it.playerId)}" }
    fun searchKey(value: String) = Normalizer.normalize(value.trim().lowercase(java.util.Locale.ROOT), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
    fun players(rows: List<Player>, query: String, position: PlayerPosition?): List<Player> {
        val term = searchKey(query)
        return rows.filter { (position == null || it.position == position) &&
            (term.isBlank() || searchKey("${it.number} ${it.name}").contains(term)) }.sortedBy { it.number }
    }
    fun matches(rows: List<LiveMatch>, query: String, status: String?): List<LiveMatch> {
        val term = searchKey(query)
        return rows.filter { (status == null || it.status == status) &&
            (term.isBlank() || searchKey(it.name).contains(term)) }.sortedWith(
            compareByDescending<LiveMatch> { it.status == "in_progress" }.thenByDescending { it.date })
    }
    fun live(rows: List<LiveMatch>) = rows.firstOrNull { it.status == "in_progress" }
    fun nextMatch(rows: List<LiveMatch>, now: Long) = rows.filter { it.status == "not_started" && it.date >= now }.minByOrNull { it.date }
    fun nextTraining(rows: List<Training>, now: Long) = rows.filter {
        it.status == TrainingStatus.EN_CURSO || (it.status == TrainingStatus.PROGRAMADO && it.date >= now)
    }.minWithOrNull(compareByDescending<Training> { it.status == TrainingStatus.EN_CURSO }.thenBy { it.date })
}
