package pe.edu.esan.sportpro.features

import org.junit.Assert.*
import org.junit.Test

class ScreenActionPolicyTest {
    @Test fun scheduledMatchOpensPreparationInsteadOfAnIntermediatePage() {
        assertEquals("liveMatch/game?prepare=true", ScreenActionPolicy.matchRoute("game", "not_started"))
    }
    @Test fun liveMatchDoesNotOpenPreparationOrAnEditor() {
        assertEquals("liveMatch/game?event=false", ScreenActionPolicy.matchRoute("game", "in_progress"))
    }
    @Test fun finishedMatchKeepsItsResultPage() {
        assertEquals("liveMatch/game?event=false", ScreenActionPolicy.matchRoute("game", "finished"))
    }
    @Test fun unknownStateDoesNotPretendTheMatchHasNotStarted() {
        assertEquals("liveMatch/game?event=false", ScreenActionPolicy.matchRoute("game", "unknown"))
        assertEquals("Ver partido", ScreenActionPolicy.matchActionLabel("unknown"))
    }
    @Test fun identifierCannotInjectAnEditorFlagOrAnotherRoute() {
        assertEquals("liveMatch/a%2Fb%3Fevent%3Dtrue%26x%3D1?prepare=true",
            ScreenActionPolicy.preparationRoute("a/b?event=true&x=1"))
    }
    @Test fun identifierSpacesDoNotBecomeQueryDelimiters() {
        assertEquals("liveMatch/game%20one?prepare=true", ScreenActionPolicy.preparationRoute("game one"))
    }
    @Test fun openingWaitsForBothMatchAndIdentity() {
        assertFalse(ScreenActionPolicy.openPreparation(true, false, false, true))
        assertFalse(ScreenActionPolicy.openPreparation(true, false, true, false))
        assertTrue(ScreenActionPolicy.openPreparation(true, false, true, true))
    }
    @Test fun dismissingPreparationDoesNotImmediatelyOpenItAgain() {
        assertFalse(ScreenActionPolicy.openPreparation(true, true, true, true))
    }
    @Test fun ordinaryEntryNeverAutoOpensPreparation() {
        for (loaded in listOf(false, true))
            assertFalse(ScreenActionPolicy.openPreparation(false, false, loaded, loaded))
    }
    @Test fun labelsDistinguishPreparationLiveAndFinished() {
        assertEquals("Abrir convocatoria", ScreenActionPolicy.matchActionLabel("not_started"))
        assertEquals("Ver en vivo", ScreenActionPolicy.matchActionLabel("in_progress"))
        assertEquals("Ver resultado", ScreenActionPolicy.matchActionLabel("finished"))
    }
}
