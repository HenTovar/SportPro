package pe.edu.esan.sportpro.data.model

/** Contact data is projected only after an authorized SERVER read, never from code receipts. */
object PlayerPersonalFields {
    fun apply(player: Player, fields: Map<String, Any?>?): Player = player.copy(
        name = fields?.get("name") as? String ?: player.name,
        phone = fields?.get("phone") as? String ?: "",
        dateOfBirth = (fields?.get("dateOfBirth") as? Number)?.toLong() ?: 0L,
        height = (fields?.get("heightCm") as? Number)?.toFloat() ?: 0f,
        weight = (fields?.get("weightKg") as? Number)?.toFloat() ?: 0f,
        emergencyContactName = "", emergencyContactPhone = "")
}
