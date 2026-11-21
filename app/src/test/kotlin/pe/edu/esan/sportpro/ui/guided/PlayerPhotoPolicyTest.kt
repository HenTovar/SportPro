package pe.edu.esan.sportpro.ui.guided

import org.junit.Assert.*
import org.junit.Test
import pe.edu.esan.sportpro.data.model.*

class PlayerPhotoPolicyTest {
    private val admin = User(uid = "admin", role = UserRole.ADMIN)
    @Test fun activeAdministratorCanUseTheExistingPhotoFieldPermission() {
        assertTrue(PlayerPhotoPolicy.canManage(admin, "team", "player"))
    }
    @Test fun inactiveAdministratorCannotUpload() {
        assertFalse(PlayerPhotoPolicy.canManage(admin.copy(isActive = false), "team", "player"))
    }
    @Test fun missingSessionOrIdentityCannotUpload() {
        assertFalse(PlayerPhotoPolicy.canManage(null, "team", "player"))
        assertFalse(PlayerPhotoPolicy.canManage(admin.copy(uid = ""), "team", "player"))
    }
    @Test fun assignedCoachDoesNotAcquireANewPhotoPermission() {
        assertFalse(PlayerPhotoPolicy.canManage(User(uid = "coach", role = UserRole.ENTRENADOR,
            coachTeamIds = listOf("team")), "team", "player"))
    }
    @Test fun playerAndParentDoNotAcquirePhotoWritePermission() {
        for (role in listOf(UserRole.JUGADOR, UserRole.PADRE))
            assertFalse(PlayerPhotoPolicy.canManage(User(uid = "member", role = role,
                teamId = "team", playerId = "player"), "team", "player"))
    }
    @Test fun photoPathHasTheExistingTeamAndPlayerHierarchy() {
        assertEquals("players/team/player/new-object", PlayerPhotoPolicy.objectPath("team", "player", "new-object"))
    }
    @Test fun invalidSegmentsCannotEscapeThePlayerDirectory() {
        for (bad in listOf("", " ", ".", "..", "a/b", "a\n", "x".repeat(129))) {
            assertFalse(PlayerPhotoPolicy.canManage(admin, bad, "player"))
            assertFalse(PlayerPhotoPolicy.canManage(admin, "team", bad))
            for (parts in listOf(listOf(bad, "player", "object"), listOf("team", bad, "object"), listOf("team", "player", bad))) {
                assertTrue(runCatching { PlayerPhotoPolicy.objectPath(parts[0], parts[1], parts[2]) }.isFailure)
            }
        }
    }
    @Test fun fileAtStorageLimitIsRejectedBeforeUpload() {
        assertTrue(PlayerPhotoPolicy.validImage("image/jpeg", PlayerPhotoPolicy.maxBytesExclusive - 1))
        assertFalse(PlayerPhotoPolicy.validImage("image/jpeg", PlayerPhotoPolicy.maxBytesExclusive))
        assertFalse(PlayerPhotoPolicy.validImage("image/jpeg", PlayerPhotoPolicy.maxBytesExclusive + 1))
    }
    @Test fun emptyAndNegativeFileSizesAreRejected() {
        assertFalse(PlayerPhotoPolicy.validImage("image/jpeg", 0))
        assertFalse(PlayerPhotoPolicy.validImage("image/jpeg", -1))
    }
    @Test fun unknownSizeStillLeavesTheExistingServerSizeGuardInCharge() {
        assertTrue(PlayerPhotoPolicy.validImage("image/png", null))
    }
    @Test fun nonImagesAndMissingMimeAreRejected() {
        for (mime in listOf(null, "", "image/", "application/pdf", "text/plain"))
            assertFalse(PlayerPhotoPolicy.validImage(mime, 100))
    }
}
