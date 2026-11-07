package pe.edu.esan.sportpro.data.model

import org.junit.Assert.*
import org.junit.Test

class MatchFactsTest {
    private val match = LiveMatch(id="m",teamA="a",teamB="b",status="in_progress")
    private val goal = MatchEvent(id="1",typeId="goal",typeLabel="Gol",countsGoal=true,teamId="a")
    @Test fun unknownMinuteAndPlayerAreAllowedWithoutInventingThem() {
        assertNull(MatchFacts.validate("", "a", match, "", ""))
        val facts=MatchFacts.aiFacts(match,listOf(goal))
        assertTrue(facts.contains("minuto desconocido")); assertTrue(facts.contains("jugador desconocido"))
        assertTrue(facts.contains("in_progress")); assertTrue(facts.contains("1-0"))
    }
    @Test fun correctionAndVoidRecomputeScore() {
        assertEquals(1 to 0,MatchFacts.score(match,listOf(goal)))
        assertEquals(0 to 1,MatchFacts.score(match,listOf(goal.copy(teamId="b"))))
        assertEquals(0 to 0,MatchFacts.score(match,listOf(goal.copy(voided=true))))
        assertFalse(MatchFacts.aiFacts(match,listOf(goal.copy(voided=true))).contains("Evento 1"))
    }
    @Test fun lateArrivalIsOrderedByMatchMinuteAndUnknownLast() {
        val late=goal.copy(id="late",minute=2,recordedAt=999)
        val early=goal.copy(id="early",minute=10,recordedAt=1)
        assertEquals(listOf("late","early","1"),MatchFacts.timeline(listOf(goal,early,late)).map{it.id})
    }
    @Test fun hashIsOrderIndependentAndEveryCorrectionInvalidatesReview() {
        val second=goal.copy(id="2",minute=8)
        assertEquals(MatchFacts.hash(listOf(goal,second)),MatchFacts.hash(listOf(second,goal)))
        for(change in listOf(goal.copy(voided=true),goal.copy(note="Dato"),goal.copy(revision=1),goal.copy(player="9"),goal.copy(teamId="b")))
            assertNotEquals(MatchFacts.hash(listOf(goal)),MatchFacts.hash(listOf(change)))
    }
    @Test fun minuteBoundariesAndForeignTeamsFail() {
        assertEquals(0,MatchFacts.validate("0","a",match,"",""));assertEquals(180,MatchFacts.validate("180","b",match,"",""))
        for(value in listOf("-1","181","texto")) {
            try { MatchFacts.validate(value,"a",match,"","");fail("accepted $value") } catch(expected:IllegalArgumentException) {}
        }
        try {MatchFacts.validate("2","foreign",match,"","");fail("foreign team")}catch(expected:IllegalArgumentException){}
    }
}
