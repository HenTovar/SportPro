package pe.edu.esan.sportpro.features

import org.junit.Assert.*
import org.junit.Test
import pe.edu.esan.sportpro.data.model.*

class AlertTrackerTest {
    private val pending=LiveMatch(id="m",status="not_started")
    private val live=pending.copy(status="in_progress")
    private val goal=MatchEvent(id="g",countsGoal=true)
    @Test fun initialServerAndCacheNeverReplayStart() {val tracker=AlertTracker();assertTrue(tracker.matches(listOf(live),false).isEmpty());assertTrue(tracker.matches(listOf(live),true).isEmpty())}
    @Test fun startIsEmittedOnceOnConfirmedTransition() {val t=AlertTracker();t.matches(listOf(pending),true);assertEquals(1,t.matches(listOf(live),true).size);assertEquals(0,t.matches(listOf(live),true).size)}
    @Test fun pendingTransitionWaitsForServer() {val t=AlertTracker();t.matches(listOf(pending),true);assertEquals(0,t.matches(listOf(live),false).size);assertEquals(1,t.matches(listOf(live),true).size)}
    @Test fun initialHistoricalGoalsSilent() {assertTrue(AlertTracker().events("m",listOf(goal),true).isEmpty())}
    @Test fun newlyConfirmedGoalEmitsOnce() {val t=AlertTracker();t.events("m",emptyList(),true);assertEquals(1,t.events("m",listOf(goal),true).size);assertEquals(0,t.events("m",listOf(goal),true).size)}
    @Test fun cachedGoalWaitsForConfirmation() {val t=AlertTracker();t.events("m",emptyList(),true);assertTrue(t.events("m",listOf(goal),false).isEmpty());assertEquals(1,t.events("m",listOf(goal),true).size)}
    @Test fun voidAndScorerCorrectionNotExtraGoal() {val t=AlertTracker();t.events("m",listOf(goal),true);assertTrue(t.events("m",listOf(goal.copy(player="different",revision=1)),true).isEmpty());assertTrue(t.events("m",listOf(goal.copy(voided=true,revision=2)),true).isEmpty())}
    @Test fun nonGoalNeverNotifies() {val t=AlertTracker();t.events("m",emptyList(),true);assertTrue(t.events("m",listOf(goal.copy(countsGoal=false)),true).isEmpty())}
    @Test fun feedsHaveSeparateBaselines() {val t=AlertTracker();t.events("a",emptyList(),true);assertTrue(t.events("b",listOf(goal),true).isEmpty());assertEquals(1,t.events("a",listOf(goal),true).size)}
    @Test fun trainingInitialSilentAndChangedOnce() {val t=AlertTracker();val session=Training(id="t",date=100);assertTrue(t.trainings(listOf(session),true).isEmpty());assertEquals(1,t.trainings(listOf(session.copy(date=200)),true).size);assertTrue(t.trainings(listOf(session.copy(date=200)),true).isEmpty())}
    @Test fun pendingTrainingNotNotified() {val t=AlertTracker();val a=Training(id="t",date=1);t.trainings(listOf(a),true);assertTrue(t.trainings(listOf(a.copy(date=2)),false).isEmpty());assertEquals(1,t.trainings(listOf(a.copy(date=2)),true).size)}
    @Test fun removedTrainingNotifiesOnce() {val t=AlertTracker();t.trainings(listOf(Training(id="t")),true);assertEquals(1,t.trainings(emptyList(),true).size);assertTrue(t.trainings(emptyList(),true).isEmpty())}
    @Test fun reattachedFeedGetsFreshSilentBaseline() {val t=AlertTracker();t.events("m",emptyList(),true);t.forgetMatch("m");assertTrue(t.events("m",listOf(goal),true).isEmpty())}
    @Test fun newLiveMatchAfterBaselineNotifies() {val t=AlertTracker();t.matches(emptyList(),true);assertEquals(1,t.matches(listOf(live),true).size)}
    @Test fun subscriptionResetDoesNotLeakOtherTeamBaseline() {val first=AlertTracker();first.events("m",emptyList(),true);assertTrue(AlertTracker().events("m",listOf(goal),true).isEmpty())}
}
