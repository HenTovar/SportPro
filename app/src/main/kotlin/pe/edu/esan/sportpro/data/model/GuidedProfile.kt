package pe.edu.esan.sportpro.data.model

/** Form fields only. requestedRole is NEVER an authorization claim. */
data class GuidedProfile(
    val requestedRole: UserRole = UserRole.JUGADOR,
    val name: String = "",
    val phone: String = "",
    val dateOfBirth: Long? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val position: PlayerPosition? = null,
    val dominantFoot: String? = null,
    val specialtyOrLicense: String = "",
    val yearsExperience: Int? = null,
    val coachPending: Boolean = false
) {
    fun validate(requireSports: Boolean = true) {
        require(name.isNotBlank() && name.length <= 120) { "Completa el nombre" }
        require(phone.isNotBlank() && phone.length <= 40) { "Completa el teléfono" }
        require(heightCm == null || (heightCm.isFinite() && heightCm in 30.0..260.0)) { "Estatura no válida" }
        require(weightKg == null || (weightKg.isFinite() && weightKg in 1.0..500.0)) { "Peso no válido" }
        when (requestedRole) {
            UserRole.JUGADOR -> {
                require(dateOfBirth != null && dateOfBirth > 0L && dateOfBirth <= System.currentTimeMillis()) { "Completa la fecha de nacimiento" }
                require(!requireSports || (position != null && dominantFoot in listOf("RIGHT", "LEFT", "BOTH"))) { "Completa posición y pie hábil" }
            }
            UserRole.ENTRENADOR -> {
                require(specialtyOrLicense.isNotBlank() && specialtyOrLicense.length <= 200) { "Completa especialidad o licencia" }
                require(yearsExperience != null && yearsExperience in 0..80) { "Completa años de experiencia" }
            }
            UserRole.PADRE -> Unit
            UserRole.ADMIN -> Unit // existing administrator editing, never a registration choice
        }
    }
}

data class TeamJoiner(val uid: String, val playerId: String, val name: String, val joinedAtMillis: Long)
