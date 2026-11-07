package pe.edu.esan.sportpro.ui.navigation

import org.junit.Assert.*
import org.junit.Test

class PhoneChromePolicyTest {
    @Test fun shortWallLabelDoesNotChangeDestination() {
        assertEquals("Muro", StructurePolicy.navigationLabel("Comunidad"))
        assertEquals("Equipo", StructurePolicy.navigationLabel("Equipo"))
        assertEquals("Perfil", StructurePolicy.navigationLabel("Perfil"))
    }
    @Test fun primaryRoutesNeverShowBack() {
        for (route in listOf("home", "profile", "teams", "teams?pick={pick}", "team/{teamId}?tab={tab}", "matches/{teamId}", "community/{teamId}", "matches/real-team"))
            assertTrue(route, StructurePolicy.isPrimaryDestination(route))
    }
    @Test fun detailsKeepBack() {
        for (route in listOf("liveMatch/{matchId}?event={event}", "editPlayer/{teamId}/{playerId}", "attendance/{teamId}/{trainingId}", "createTeam", "adminRequests", "players/{teamId}", ""))
            assertFalse(route, StructurePolicy.isPrimaryDestination(route))
    }
}
