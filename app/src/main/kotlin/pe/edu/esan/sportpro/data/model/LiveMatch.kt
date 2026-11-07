package pe.edu.esan.sportpro.data.model

import java.security.MessageDigest

data class LiveMatch(val id: String = "", val name: String = "Partido", val teamA: String = "",
    val teamB: String = "", val status: String = "not_started", val date: Long = 0, val eventVersion: Long = 0)
data class EventType(val id: String = "", val label: String = "", val countsGoal: Boolean = false,
    val active: Boolean = true)
data class MatchEvent(val id: String = "", val typeId: String = "", val typeLabel: String = "",
    val countsGoal: Boolean = false, val minute: Int? = null, val teamId: String = "",
    val player: String = "", val note: String = "", val voided: Boolean = false,
    val revision: Long = 0, val revisionId: String = "", val createdBy: String = "",
    val updatedBy: String = "", val recordedAt: Long = 0)
data class EventRevision(val id: String, val actorUid: String, val at: Long,
    val previous: String, val current: String)
data class MatchFeed(val match: LiveMatch? = null, val events: List<MatchEvent> = emptyList(),
    val types: List<EventType> = emptyList(), val cached: Boolean = true, val pending: Boolean = false)
data class Narrative(val text: String = "", val sourceHash: String = "", val state: String = "DRAFT",
    val authorUid: String = "", val approvedBy: String = "", val sourceVersion: Long = 0)
object MatchFacts {
    fun validate(minute: String, teamId: String, match: LiveMatch, note: String, player: String): Int? {
        require(teamId == match.teamA || teamId == match.teamB) { "Elige un equipo del partido" }
        require(note.length <= 300 && player.length <= 80) { "Acorta la observación o identificación" }
        if (minute.isBlank()) return null
        return minute.toIntOrNull()?.takeIf { it in 0..180 }
            ?: throw IllegalArgumentException("Minuto entre 0 y 180 o vacío si se desconoce")
    }
    fun score(match: LiveMatch, events: List<MatchEvent>): Pair<Int,Int> =
        events.count { !it.voided && it.countsGoal && it.teamId == match.teamA } to
        events.count { !it.voided && it.countsGoal && it.teamId == match.teamB }
    fun timeline(events: List<MatchEvent>) = events.sortedWith(
        compareBy<MatchEvent> { it.minute ?: Int.MAX_VALUE }.thenBy { it.recordedAt }.thenBy { it.id })
    // Source hash includes revisions and voids: a correction invalidates an old review.
    fun hash(events: List<MatchEvent>): String {
        val text = events.sortedBy { it.id }.joinToString("\n") {
            listOf(it.id,it.revisionId,it.revision,it.typeId,it.typeLabel,it.countsGoal,it.minute,
                it.teamId,it.player,it.note,it.voided).joinToString("|") { value ->
                val s = value?.toString().orEmpty(); "${s.length}:$s"
            }
        }
        return MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    }
    fun aiFacts(match: LiveMatch, events: List<MatchEvent>): String =
        pe.edu.esan.sportpro.data.network.AnonymousNarrative.prepare(match,events).prompt
}
