package pe.edu.esan.sportpro.ui.guided

import pe.edu.esan.sportpro.data.model.GuidedProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun GuidedProfile.asDraft() = GuidedFormDraft(
    role = requestedRole, name = name, phone = phone,
    birthDate = dateOfBirth?.let { SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("UTC") }.format(Date(it)) }.orEmpty(),
    heightCm = heightCm?.toString().orEmpty(), weightKg = weightKg?.toString().orEmpty(),
    position = position, foot = FootChoice.values().firstOrNull { it.name == dominantFoot },
    specialtyOrLicense = specialtyOrLicense, yearsExperience = yearsExperience?.toString().orEmpty()
)

fun GuidedFormDraft.asProfile(previous: GuidedProfile? = null) = GuidedProfile(
    requestedRole = checkNotNull(role), name = name.trim(), phone = phone.trim(),
    dateOfBirth = GuidedFormDraft.parseBirthDate(birthDate),
    heightCm = GuidedFormDraft.optionalNumber(heightCm), weightKg = GuidedFormDraft.optionalNumber(weightKg),
    position = position, dominantFoot = foot?.name,
    specialtyOrLicense = specialtyOrLicense.trim(), yearsExperience = yearsExperience.toIntOrNull(),
    coachPending = previous?.coachPending ?: false
)
