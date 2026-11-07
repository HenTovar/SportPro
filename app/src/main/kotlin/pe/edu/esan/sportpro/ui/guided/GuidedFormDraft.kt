package pe.edu.esan.sportpro.ui.guided

import pe.edu.esan.sportpro.data.model.PlayerPosition
import pe.edu.esan.sportpro.data.model.UserRole
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

enum class ProfileField { ROLE, NAME, PHONE, BIRTH_DATE, HEIGHT, WEIGHT, POSITION, FOOT, SPECIALTY, EXPERIENCE }
enum class FormAccess { REGISTRATION, SELF, STAFF_SPORTS, ADMIN }
enum class FootChoice(val label: String) { RIGHT("Derecho"), LEFT("Izquierdo"), BOTH("Ambidiestro") }

/** Transient UI state only. Passwords and email are deliberately outside this form DTO. */
data class GuidedFormDraft(
    val role: UserRole? = null,
    val name: String = "",
    val phone: String = "",
    val birthDate: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val position: PlayerPosition? = null,
    val foot: FootChoice? = null,
    val specialtyOrLicense: String = "",
    val yearsExperience: String = ""
) {
    fun selectRole(next: UserRole): GuidedFormDraft {
        require(next in listOf(UserRole.JUGADOR, UserRole.PADRE, UserRole.ENTRENADOR))
        // Never carry hidden, role-specific personal data into another kind of registration.
        return GuidedFormDraft(role = next, name = name, phone = phone)
    }

    fun canEdit(field: ProfileField, access: FormAccess): Boolean = when (access) {
        FormAccess.REGISTRATION -> true
        FormAccess.ADMIN -> field != ProfileField.ROLE
        FormAccess.STAFF_SPORTS -> field in setOf(ProfileField.POSITION, ProfileField.FOOT)
        FormAccess.SELF -> field !in setOf(ProfileField.ROLE, ProfileField.POSITION, ProfileField.FOOT)
    }

    fun errors(access: FormAccess, nowMillis: Long = System.currentTimeMillis()): Map<ProfileField, String> {
        val errors = linkedMapOf<ProfileField, String>()
        if (role == null || role == UserRole.ADMIN && access == FormAccess.REGISTRATION) {
            errors[ProfileField.ROLE] = "Elige Jugador, Padre/tutor o Entrenador."
            return errors
        }
        if (access != FormAccess.STAFF_SPORTS) {
            if (name.isBlank() || name.trim().length > 120) errors[ProfileField.NAME] = "Escribe tu nombre completo (máximo 120 caracteres)."
            val contact = phone.trim()
            if (contact.length > 30 || !contact.matches(Regex("[+0-9(). -]+")) || contact.count { it in '0'..'9' } !in 7..15)
                errors[ProfileField.PHONE] = "Escribe un teléfono de contacto válido."
        }
        if (role == UserRole.JUGADOR) {
            if (access != FormAccess.STAFF_SPORTS) {
                val birth = parseBirthDate(birthDate)
                if (birth == null || birth <= 0L || birth > nowMillis) errors[ProfileField.BIRTH_DATE] = "Usa una fecha válida, no futura: aaaa-mm-dd."
                if (!validOptionalRange(heightCm, 30.0..260.0)) errors[ProfileField.HEIGHT] = "Usa una estatura de 30 a 260 cm o deja el campo vacío."
                if (!validOptionalRange(weightKg, 1.0..500.0)) errors[ProfileField.WEIGHT] = "Usa un peso de 1 a 500 kg o deja el campo vacío."
            }
            if (access != FormAccess.SELF) {
                if (position == null) errors[ProfileField.POSITION] = "Selecciona tu posición."
                if (foot == null) errors[ProfileField.FOOT] = "Selecciona tu pie hábil."
            }
        }
        if (role == UserRole.ENTRENADOR && access != FormAccess.STAFF_SPORTS) {
            if (specialtyOrLicense.isBlank() || specialtyOrLicense.trim().length > 200)
                errors[ProfileField.SPECIALTY] = "Escribe tu especialidad o licencia (máximo 200 caracteres)."
            if (!yearsExperience.matches(Regex("[0-9]+")) || yearsExperience.toIntOrNull()?.let { it in 0..80 } != true)
                errors[ProfileField.EXPERIENCE] = "Escribe los años de experiencia entre 0 y 80; cero es válido."
        }
        return errors
    }

    companion object {
        fun parseBirthDate(value: String): Long? {
            if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(value)) return null
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false; timeZone = TimeZone.getTimeZone("UTC") }
            val cursor = ParsePosition(0)
            return format.parse(value, cursor)?.takeIf { cursor.index == value.length }?.time
        }
        fun validOptionalRange(value: String, range: ClosedFloatingPointRange<Double>): Boolean = value.isBlank() ||
            value.trim().toDoubleOrNull()?.let { it.isFinite() && it in range } == true
        fun optionalNumber(value: String): Double? = value.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
    }
}
