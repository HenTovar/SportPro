package pe.edu.esan.sportpro.data.model

import org.junit.Assert.*
import org.junit.Test

class RolePolicyTest {
    @Test fun registrationNeverOffersAdmin() {
        assertEquals(setOf(UserRole.JUGADOR, UserRole.PADRE), RolePolicy.registrationRoles.toSet())
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
            assertTrue(RolePolicy.canReadPlayer(User(role = role, coachTeamIds = listOf("team")), "team", "player"))
        }
        assertFalse(RolePolicy.isStaff(null))
    }
    @Test fun coachChoiceCreatesOnlyBasePlayer() {
        assertTrue(UserRole.ENTRENADOR in RolePolicy.registrationChoices)
        assertFalse(UserRole.ENTRENADOR in RolePolicy.registrationRoles)
        assertEquals(UserRole.JUGADOR,RolePolicy.registrationBaseRole(UserRole.ENTRENADOR))
        assertEquals(UserRole.PADRE,RolePolicy.registrationBaseRole(UserRole.PADRE))
    }
    @Test fun adminCanNeverBeSelectedAsBaseRole() {
        assertThrows(IllegalArgumentException::class.java) { RolePolicy.registrationBaseRole(UserRole.ADMIN) }
    }
    @Test fun creatorWithoutAdminCannotGrantCoach() {
        for(role in listOf(UserRole.JUGADOR,UserRole.PADRE,UserRole.ENTRENADOR))
            assertFalse(RolePolicy.canApprove(role,true,UserRole.ENTRENADOR))
        assertTrue(RolePolicy.canApprove(UserRole.ADMIN,true,UserRole.ENTRENADOR))
    }
    @Test fun approvalClearsFamilyLegacyAndCoachScopeAtomically() {
        assertEquals(mapOf("role" to "JUGADOR", "children" to emptyMap<String,Any>(), "teamId" to "", "playerId" to "", "coachTeamIds" to emptyList<String>()),RolePolicy.approvalFields(UserRole.JUGADOR))
        assertEquals(mapOf("role" to "PADRE", "children" to emptyMap<String,Any>(), "teamId" to "", "playerId" to "", "coachTeamIds" to emptyList<String>()),RolePolicy.approvalFields(UserRole.PADRE))
    }

    @Test fun coachNeedsAnExplicitDerivedTeamAndCannotReadForeignPlayers() {
        val coach = User(role=UserRole.ENTRENADOR, coachTeamIds=listOf("own"))
        assertTrue(RolePolicy.canReadTeam(coach,"own"))
        assertFalse(RolePolicy.canReadTeam(coach,"other"))
        assertFalse(RolePolicy.canReadPlayer(coach,"other","child"))
        assertFalse(RolePolicy.canReadTeam(coach.copy(coachTeamIds=emptyList(),teamId="own"),"own"))
        assertFalse(RolePolicy.canReadTeam(coach.copy(isActive=false),"own"))
    }
    @Test fun revocationMarkerDisablesLegacyButNotFreshFamilyGrant() {
        val parent=User(role=UserRole.PADRE,teamId="a",playerId="old",hasFamilyRevocation=true)
        assertFalse(RolePolicy.canReadPlayer(parent,"a","old"))
        assertFalse(RolePolicy.canReadTeam(parent,"a"))
        assertTrue(RolePolicy.canReadPlayer(parent.copy(children=listOf(FamilyChild("b","new","Nuevo"))),"b","new"))
    }
}
