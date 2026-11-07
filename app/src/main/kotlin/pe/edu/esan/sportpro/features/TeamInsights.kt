package pe.edu.esan.sportpro.features

import pe.edu.esan.sportpro.data.model.*
import java.util.Calendar
import java.util.TimeZone
import java.util.Locale

data class Month(val year: Int, val number: Int) {
    init { require(number in 1..12 && year in 1970..9999) }
    fun shift(delta: Int): Month { val c = calendar(); c.add(Calendar.MONTH, delta); return Month(c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1) }
    fun calendar(zone: TimeZone = TimeZone.getDefault()): Calendar = Calendar.getInstance(zone).apply {
        clear(); set(year, number-1, 1, 12, 0, 0)
    }
    fun days(zone: TimeZone = TimeZone.getDefault()) = calendar(zone).getActualMaximum(Calendar.DAY_OF_MONTH)
    fun leading(zone: TimeZone = TimeZone.getDefault()) = (calendar(zone).get(Calendar.DAY_OF_WEEK)+5)%7
    fun contains(millis: Long, zone: TimeZone = TimeZone.getDefault()): Boolean {
        if (millis <= 0) return false
        val c = Calendar.getInstance(zone).apply { timeInMillis = millis }
        return c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH)+1 == number
    }
    companion object { fun now(): Month = Calendar.getInstance().let { Month(it.get(Calendar.YEAR),it.get(Calendar.MONTH)+1) } }
}
data class AgendaEntry(val id: String, val title: String, val at: Long, val kind: String, val route: String)
data class AttendanceSummary(val present: Int, val absent: Int, val other: Int, val unregistered: Int) {
    val total get() = present+absent+other
    val percent get() = if(total == 0) null else (present*100.0/total).toInt()
}
data class Streak(val result: String, val count: Int)
object TeamInsights {
    fun goals(team: String, events: Map<String,List<MatchEvent>>): List<Pair<String,Int>> = events.values.flatten()
        .filter { !it.voided && it.countsGoal && it.teamId == team }
        .groupBy { it.player.trim().ifBlank { "Sin identificar" }.lowercase(Locale.ROOT) }
        .map { (_, rows) -> rows.first().player.trim().ifBlank { "Sin identificar" } to rows.size }
        .sortedWith(compareByDescending<Pair<String,Int>> { it.second }.thenBy { it.first })
    fun attendance(month: Month, rows: List<AttendanceRecord>, trainings: List<Training>, zone: TimeZone = TimeZone.getDefault()): AttendanceSummary {
        val dates = trainings.associate { it.id to it.date }
        val latest = rows.sortedByDescending { it.recordedAt }.distinctBy { it.playerId to it.trainingId }
        val selected = latest.filter { month.contains(dates[it.trainingId] ?: 0,zone) }
        return AttendanceSummary(selected.count { it.status == AttendanceStatus.ASISTIO }, selected.count { it.status == AttendanceStatus.FALTA },
            selected.count { it.status !in listOf(AttendanceStatus.ASISTIO,AttendanceStatus.FALTA) }, latest.count { (dates[it.trainingId] ?: 0) <= 0 })
    }
    fun streak(team: String, matches: List<LiveMatch>, events: Map<String,List<MatchEvent>>): Streak {
        val results = matches.filter { it.status == "finished" && it.date > 0 && (it.teamA == team || it.teamB == team) && events.containsKey(it.id) }
            .sortedByDescending { it.date }.map { match ->
                val (a,b)=MatchFacts.score(match,events[match.id].orEmpty()); val own=if(match.teamA==team)a else b; val rival=if(match.teamA==team)b else a
                when { own>rival -> "victorias"; own<rival -> "derrotas"; else -> "empates" }
            }
        return if(results.isEmpty()) Streak("sin partidos finalizados",0) else Streak(results.first(),results.takeWhile { it==results.first() }.size)
    }
    fun agenda(matches: List<LiveMatch>, trainings: List<Training>): List<AgendaEntry> =
        (matches.filter { it.date>0 }.map { AgendaEntry("match/"+it.id,it.name,it.date,"Partido · "+it.status,"liveMatch/"+java.net.URLEncoder.encode(it.id,"UTF-8").replace("+","%20")) } +
        trainings.filter { it.date>0 }.map { AgendaEntry("training/"+it.id,it.name,it.date,"Entrenamiento · "+it.status.name,"training") }).sortedBy { it.at }
}

data class LocalAlert(val key: String, val title: String, val body: String)
/** Baselines are per subscription. Cached/pending data never creates historical or unconfirmed alerts. */
class AlertTracker {
    private var matches: Map<String,LiveMatch>? = null
    private var trainings: Map<String,Training>? = null
    private val events = mutableMapOf<String,Map<String,MatchEvent>>()
    fun matches(rows: List<LiveMatch>, confirmed: Boolean): List<LocalAlert> {
        if(!confirmed) return emptyList()
        val old=matches; matches=rows.associateBy { it.id }
        return rows.filter { old != null && it.status == "in_progress" && (old[it.id] == null || old[it.id]?.status == "not_started") }
            .map { LocalAlert("start/"+it.id,"Comenzó un partido","Tu equipo tiene un partido en vivo. Abre la app para verlo.") }
    }
    fun forgetMatch(id: String) { events.remove(id) }
    fun events(match: String, rows: List<MatchEvent>, confirmed: Boolean): List<LocalAlert> {
        if(!confirmed) return emptyList()
        val old=events.put(match,rows.associateBy { it.id }) ?: return emptyList()
        return rows.filter { !it.voided && it.countsGoal && (old[it.id]?.let { before -> before.voided || !before.countsGoal } ?: true) }
            .map { LocalAlert("goal/$match/"+it.id+"/"+it.revision,"Gol registrado","Hay una actualización del marcador. Abre el partido para ver los detalles.") }
    }
    fun trainings(rows: List<Training>, confirmed: Boolean): List<LocalAlert> {
        if(!confirmed) return emptyList()
        val old=trainings; trainings=rows.associateBy { it.id }; if(old==null) return emptyList()
        return rows.filter { old[it.id] != it }.map {
            LocalAlert("training/"+it.id+"/"+it.updatedAt,"Cambio en entrenamiento","La agenda de tu equipo cambió. Revisa la fecha y las indicaciones en la app.")
        } + old.keys.filter { id -> rows.none { it.id == id } }.map { id ->
            LocalAlert("training/removed/"+id,"Entrenamiento retirado","Un entrenamiento dejó de figurar en tu agenda. Revisa la app.")
        }
    }
}

data class FeatureState(val team: String = "", val matches: List<LiveMatch> = emptyList(), val events: Map<String,List<MatchEvent>> = emptyMap(),
    val trainings: List<Training> = emptyList(), val attendance: List<AttendanceRecord> = emptyList(), val cached: Boolean = true,
    val pending: Boolean = false, val loading: Boolean = true, val error: String? = null)
