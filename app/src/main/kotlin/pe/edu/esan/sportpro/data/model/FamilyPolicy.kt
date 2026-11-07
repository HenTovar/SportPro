package pe.edu.esan.sportpro.data.model

import java.security.SecureRandom

data class FamilyChild(val teamId: String = "", val playerId: String = "", val name: String = "") {
    val key: String get() = "$teamId/$playerId"
}

object FamilyPolicy {
    private const val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    fun normalizeCode(value: String) = value.trim().uppercase(java.util.Locale.ROOT).replace("-", "").replace(" ", "")
    fun validCode(code: String) = code.length == 12 && code.all { it in alphabet }
    fun newCode(random: SecureRandom = SecureRandom()) = (1..12).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
    fun children(user: User?): List<FamilyChild> {
        if (user == null || !user.isActive || user.role !in listOf(UserRole.PADRE, UserRole.JUGADOR)) return emptyList()
        val legacy = if (user.teamId.isNotBlank() && user.playerId.isNotBlank())
            listOf(FamilyChild(user.teamId, user.playerId, "Ficha vinculada")) else emptyList()
        return (legacy + if (user.role == UserRole.PADRE) user.children else emptyList())
            .filter { it.teamId.isNotBlank() && it.playerId.isNotBlank() }.distinctBy { it.key }
    }
    fun selected(user: User?, remembered: String): FamilyChild? = children(user).let { rows -> rows.find { it.key == remembered } ?: rows.firstOrNull() }
    fun decode(raw: Any?): List<FamilyChild> = (raw as? Map<*, *>)?.flatMap { (team, players) ->
        (players as? Map<*, *>)?.mapNotNull { (player, fields) ->
            if (team !is String || player !is String || fields !is Map<*, *>) null
            else FamilyChild(team, player, fields["name"] as? String ?: "Ficha vinculada")
        }.orEmpty()
    }.orEmpty()
}
