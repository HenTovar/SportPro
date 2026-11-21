package pe.edu.esan.sportpro.ui.guided

import pe.edu.esan.sportpro.data.model.RolePolicy
import pe.edu.esan.sportpro.data.model.User
import pe.edu.esan.sportpro.data.model.UserRole

/** Mirrors the existing photo-field restriction; it does not grant a server permission. */
object PlayerPhotoPolicy {
    const val maxBytesExclusive = 5L * 1024 * 1024

    private fun segment(value: String) = value.isNotBlank() && value.length <= 128 &&
        value != "." && value != ".." && value.none { it == '/' || it.isISOControl() }

    fun canManage(actor: User?, teamId: String, playerId: String) =
        segment(teamId) && segment(playerId) && actor != null && actor.role == UserRole.ADMIN &&
            actor.uid.isNotBlank() && RolePolicy.canReadPlayer(actor, teamId, playerId)

    fun objectPath(teamId: String, playerId: String, nonce: String): String {
        require(segment(teamId) && segment(playerId) && segment(nonce)) { "Ficha no válida." }
        // Matches the existing per-team/per-player Storage path. Never uses the legacy flat path.
        return "players/$teamId/$playerId/$nonce"
    }

    fun validImage(mime: String?, knownSize: Long?) = mime != null && mime.startsWith("image/") && mime.length > 6 &&
        (knownSize == null || knownSize in 1L until maxBytesExclusive)
}
