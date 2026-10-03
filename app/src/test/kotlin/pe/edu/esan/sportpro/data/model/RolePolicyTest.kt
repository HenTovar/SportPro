package pe.edu.esan.sportpro.data.model

import org.junit.Assert.*
import org.junit.Test

class RolePolicyTest {
    @Test fun registrationNeverOffersAdmin() {
        assertEquals(setOf(UserRole.ENTRENADOR, UserRole.JUGADOR, UserRole.PADRE), RolePolicy.registrationRoles.toSet())
        assertFalse(UserRole.ADMIN in RolePolicy.registrationRoles)
    }
    @Test fun playerAndParentOnlySeeTheirLinkedTeamAndPlayer() {
        for (role in listOf(UserRole.JUGADOR, UserRole.PADRE)) {
            val user = User(uid = "account", role = role, teamId = "own", playerId = "child")
            assertTrue(RolePolicy.canReadTeam(user, "own"))
            assertTrue(RolePolicy.canReadPlayer(user, "own", "child"))
            assertFalse(RolePolicy.canReadTeam(user, "foreign"))
            assertFalse(RolePolicy.canReadPlayer(user, "foreign", "child"))
            assertFalse(RolePolicy.canReadPlayer(user, "own", "other"))
            assertFalse(RolePolicy.isStaff(role))
        }
    }
    @Test fun missingIdentityAndEmptyIdsFailClosed() {
        assertFalse(RolePolicy.canReadTeam(null, "team"))
        assertFalse(RolePolicy.canReadTeam(User(), "team"))
        assertFalse(RolePolicy.canReadTeam(User(role = UserRole.ADMIN), ""))
        assertFalse(RolePolicy.canReadPlayer(User(role = UserRole.ADMIN), "team", ""))
    }
    @Test fun staffCanManageTeamsWhileRestrictedRolesCannot() {
        for (role in listOf(UserRole.ADMIN, UserRole.ENTRENADOR)) {
            assertTrue(RolePolicy.isStaff(role))
            assertTrue(RolePolicy.canReadPlayer(User(role = role), "team", "player"))
        }
        assertFalse(RolePolicy.isStaff(null))
    }
}
