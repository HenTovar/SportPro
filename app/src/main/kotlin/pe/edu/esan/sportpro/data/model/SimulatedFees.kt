package pe.edu.esan.sportpro.data.model

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

enum class SimulatedFeeStatus { PENDING, PAID, EXEMPT }

data class SimulatedFeeRecord(
    val teamId: String,
    val playerId: String,
    val period: String,
    val status: SimulatedFeeStatus,
    val revision: Long,
    val createdBy: String,
    val createdAtMillis: Long?,
    val updatedBy: String,
    val updatedAtMillis: Long?
)

object SimulatedFeesPolicy {
    private val periodPattern = Regex("[1-9][0-9]{3}-(0[1-9]|1[0-2])")
    val documentKeys = setOf("teamId", "playerId", "period", "status", "simulated", "revision",
        "createdBy", "createdAt", "updatedBy", "updatedAt")

    fun validId(id: String): Boolean = id.isNotBlank() && id.length <= 128 && '/' !in id &&
        id != "." && id != ".." && id.none { it.isISOControl() }

    fun validPeriod(period: String): Boolean = periodPattern.matches(period)

    fun requirePath(teamId: String, playerId: String, period: String) {
        require(validId(teamId) && validId(playerId)) { "La ficha o el equipo no son válidos." }
        require(validPeriod(period)) { "Selecciona un período válido con formato AAAA-MM." }
    }

    fun canManage(user: User?): Boolean = user != null && user.uid.isNotBlank() &&
        user.isActive && user.role == UserRole.ADMIN

    fun canRead(user: User?, teamId: String, playerId: String): Boolean =
        validId(teamId) && validId(playerId) && user != null && user.uid.isNotBlank() &&
            user.isActive && (canManage(user) || FamilyPolicy.children(user).any {
                it.teamId == teamId && it.playerId == playerId
            })

    fun requireRevision(actual: Long, expected: Long) {
        require(actual >= 0 && expected >= 0 && actual == expected) {
            "El registro cambió. Vuelve a cargar antes de guardar."
        }
        require(actual < Long.MAX_VALUE) { "No se puede actualizar este registro. Solicita revisión." }
    }

    fun parseStatus(value: String): SimulatedFeeStatus =
        SimulatedFeeStatus.values().firstOrNull { it.name == value }
            ?: throw IllegalArgumentException("El registro tiene un estado no válido.")

    fun label(status: SimulatedFeeStatus?): String = when (status) {
        null -> "Sin registro"
        SimulatedFeeStatus.PENDING -> "Pendiente (simulado)"
        SimulatedFeeStatus.PAID -> "Pagado (simulado)"
        SimulatedFeeStatus.EXEMPT -> "Exonerado (simulado)"
    }

    fun currentPeriod(calendar: Calendar = Calendar.getInstance()): String =
        String.format(Locale.ROOT, "%04d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)

    fun movePeriod(period: String, months: Int): String {
        require(validPeriod(period)) { "Período no válido." }
        require(months in -120..120) { "Desplazamiento de período no válido." }
        val calendar = GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ROOT).apply {
            clear()
            isLenient = false
            set(period.substring(0, 4).toInt(), period.substring(5, 7).toInt() - 1, 1, 12, 0, 0)
            add(Calendar.MONTH, months)
        }
        val moved = currentPeriod(calendar)
        require(validPeriod(moved)) { "No hay otro período disponible en esa dirección." }
        return moved
    }
}
