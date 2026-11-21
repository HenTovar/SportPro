package pe.edu.esan.sportpro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.kotlin.*
import pe.edu.esan.sportpro.data.model.*
import pe.edu.esan.sportpro.data.repository.*
import pe.edu.esan.sportpro.data.repository.Result
import pe.edu.esan.sportpro.data.network.NarrativeGenerator
import pe.edu.esan.sportpro.data.network.NarrativeProvider
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class LiveEventSubmissionContractTest {
    private val instances=mutableListOf<ViewModel>()
    private val type=EventType(id="goal",label="Gol",countsGoal=true)
    private val coach=User(uid="coach",role=UserRole.ENTRENADOR,coachTeamIds=listOf("a"))
    @Before fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun cleanup() { instances.forEach { it.viewModelScope.cancel() };Dispatchers.resetMain() }
    private fun model(writer:EventWriter,user:User=coach,cached:Boolean=false):LiveMatchViewModel {
        val repo:MatchRepository=mock();val auth:AuthRepository=mock()
        whenever(auth.getCurrentUser()).thenReturn(flowOf(Result.Success(user)))
        whenever(repo.feed("match")).thenReturn(flowOf(Result.Success(MatchFeed(match=LiveMatch(id="match",teamA="a",teamB="b"),types=listOf(type),cached=cached))))
        whenever(repo.narrative(eq("match"),any())).thenReturn(flowOf(Result.Success(Narrative())))
        val generator=NarrativeGenerator(NarrativeProvider {error("Generator must not run")})
        return LiveMatchViewModel("match",repo,auth,generator,writer).also {instances.add(it)}
    }
    private fun submit(vm:LiveMatchViewModel,key:String="one",team:String="a",old:MatchEvent?=null,note:String="Nota escrita")=
        vm.saveEvent(old,type,"7",team,"9",note,false,key)
    @Test fun waitsForResultKeepsDraftAndDoubleTapUsesOneWrite()=runTest {
        val done=CompletableDeferred<Unit>();var calls=0
        val vm=model(EventWriter {_,_->calls++;done.await()});runCurrent()
        submit(vm);submit(vm);runCurrent();assertEquals(1,calls)
        assertEquals(SubmissionPhase.SAVING,vm.state.value.eventWrites["one"]?.progress?.phase)
        assertFalse(checkNotNull(vm.state.value.eventWrites["one"]).acceptedForEditor)
        assertEquals("Nota escrita",vm.state.value.eventWrites["one"]?.draft?.note)
        done.complete(Unit);runCurrent();assertEquals(SubmissionPhase.CONFIRMED,vm.state.value.eventWrites["one"]?.progress?.phase)
    }
    @Test fun rejectedEventRetainsAllFieldsAndError()=runTest {
        val vm=model(EventWriter {_,_->throw IllegalArgumentException("Denegado")});runCurrent();submit(vm);runCurrent()
        val result=checkNotNull(vm.state.value.eventWrites["one"])
        assertFalse(result.acceptedForEditor);assertEquals(SubmissionPhase.REJECTED,result.progress.phase);assertEquals("Denegado",result.progress.message)
        assertEquals("7",result.draft.minute);assertEquals("a",result.draft.team);assertEquals("9",result.draft.player);assertEquals("Nota escrita",result.draft.note)
    }
    @Test fun offlineQueueAcceptanceIsNotServerConfirmationAndLaterResolves()=runTest {
        val done=CompletableDeferred<Unit>();val vm=model(EventWriter {_,queued->queued("new-local-id");done.await()},cached=true);runCurrent()
        submit(vm);runCurrent();val queued=checkNotNull(vm.state.value.eventWrites["one"])
        assertTrue(queued.acceptedForEditor);assertEquals(SubmissionPhase.QUEUED,queued.progress.phase);assertEquals("new-local-id",queued.eventId)
        assertTrue(queued.progress.message.orEmpty().contains("pendiente"))
        done.complete(Unit);runCurrent();assertEquals(SubmissionPhase.CONFIRMED,vm.state.value.eventWrites["one"]?.progress?.phase)
    }
    @Test fun lateRejectionOfQueuedEventPreservesRecoverableDraft()=runTest {
        val done=CompletableDeferred<Unit>();val vm=model(EventWriter {_,queued->queued("local");done.await();throw IllegalArgumentException("Servidor rechazó")});runCurrent()
        submit(vm);runCurrent();assertEquals(SubmissionPhase.QUEUED,vm.state.value.eventWrites["one"]?.progress?.phase)
        done.complete(Unit);runCurrent();val rejected=checkNotNull(vm.state.value.eventWrites["one"])
        assertEquals(SubmissionPhase.REJECTED,rejected.progress.phase);assertEquals("Nota escrita",rejected.draft.note);assertEquals("local",rejected.eventId)
    }
    @Test fun separateOfflineEditorsCanQueueWithoutBlockingEachOther()=runTest {
        val done=CompletableDeferred<Unit>();var calls=0
        val vm=model(EventWriter {_,queued->queued("local-${++calls}");done.await()});runCurrent()
        submit(vm,"one");runCurrent();submit(vm,"two",note="Otra jugada");runCurrent()
        assertEquals(2,calls);assertEquals(SubmissionPhase.QUEUED,vm.state.value.eventWrites["one"]?.progress?.phase)
        assertEquals(SubmissionPhase.QUEUED,vm.state.value.eventWrites["two"]?.progress?.phase)
        done.complete(Unit);runCurrent()
    }
    @Test fun unknownOutcomeBlocksOnlyThatEditorAndIsNotRetried()=runTest {
        var calls=0;val vm=model(EventWriter {_,_->calls++;throw IOException("Lost ack")});runCurrent();submit(vm);runCurrent()
        assertEquals(SubmissionPhase.UNCERTAIN,vm.state.value.eventWrites["one"]?.progress?.phase)
        submit(vm);runCurrent();assertEquals(1,calls);assertEquals("Nota escrita",vm.state.value.eventWrites["one"]?.draft?.note)
    }
    @Test fun localQueueCanResolveAnInitialUncertainWait()=runTest {
        val done=CompletableDeferred<Unit>();var queued:((String)->Unit)?=null
        val vm=model(EventWriter {_,callback->queued=callback;done.await()});runCurrent();submit(vm);runCurrent();advanceTimeBy(10_000);runCurrent()
        assertEquals(SubmissionPhase.UNCERTAIN,vm.state.value.eventWrites["one"]?.progress?.phase)
        checkNotNull(queued).invoke("local");assertEquals(SubmissionPhase.QUEUED,vm.state.value.eventWrites["one"]?.progress?.phase)
        done.complete(Unit);runCurrent();assertEquals(SubmissionPhase.CONFIRMED,vm.state.value.eventWrites["one"]?.progress?.phase)
    }
    @Test fun confirmedReceiptCannotBeDowngradedByLateQueueCallback()=runTest {
        var queued:((String)->Unit)?=null;val vm=model(EventWriter {_,callback->queued=callback});runCurrent();submit(vm);runCurrent()
        assertEquals(SubmissionPhase.CONFIRMED,vm.state.value.eventWrites["one"]?.progress?.phase)
        checkNotNull(queued).invoke("late");assertEquals(SubmissionPhase.CONFIRMED,vm.state.value.eventWrites["one"]?.progress?.phase)
    }
    @Test fun explicitRetryAfterRejectionIgnoresAnOldAttemptCallback()=runTest {
        var calls=0;var oldCallback:((String)->Unit)?=null;val done=CompletableDeferred<Unit>()
        val vm=model(EventWriter {_,callback->if(++calls==1){oldCallback=callback;throw IllegalArgumentException("Rechazo")}else done.await()});runCurrent()
        submit(vm);runCurrent();val first=checkNotNull(vm.state.value.eventWrites["one"]).progress.id
        submit(vm);runCurrent();val retry=checkNotNull(vm.state.value.eventWrites["one"]).progress.id
        assertTrue(retry>first);checkNotNull(oldCallback).invoke("stale")
        assertEquals(SubmissionPhase.SAVING,vm.state.value.eventWrites["one"]?.progress?.phase)
        done.complete(Unit);runCurrent()
    }
    @Test fun cancelledTaskKeepsDraftAndNeverReportsAccepted()=runTest {
        val vm=model(EventWriter {_,_->throw CancellationException("Task cancelled")});runCurrent();submit(vm);runCurrent()
        val receipt=checkNotNull(vm.state.value.eventWrites["one"])
        assertEquals(SubmissionPhase.UNCERTAIN,receipt.progress.phase);assertEquals("Nota escrita",receipt.draft.note)
        assertFalse(receipt.acceptedForEditor)
    }
    @Test fun anotherEditorResultDoesNotReplaceCurrentDraft()=runTest {
        val first=CompletableDeferred<Unit>();val second=CompletableDeferred<Unit>()
        val vm=model(EventWriter {draft,queued->queued(draft.note);if(draft.note=="first")first.await() else second.await()});runCurrent()
        submit(vm,"one",note="first");runCurrent();submit(vm,"two",note="second");runCurrent();first.complete(Unit);runCurrent()
        assertEquals("second",vm.state.value.eventWrites["two"]?.draft?.note);assertEquals(SubmissionPhase.QUEUED,vm.state.value.eventWrites["two"]?.progress?.phase)
        second.complete(Unit);runCurrent()
    }
    @Test fun coachCannotWriteRivalOrMoveRivalEventIntoOwnTeam()=runTest {
        var calls=0;val vm=model(EventWriter {_,_->calls++});runCurrent()
        submit(vm,"rival",team="b");submit(vm,"move",old=MatchEvent(id="old",teamId="b"));runCurrent()
        assertEquals(0,calls);assertEquals(SubmissionPhase.REJECTED,vm.state.value.eventWrites["rival"]?.progress?.phase)
        assertEquals(SubmissionPhase.REJECTED,vm.state.value.eventWrites["move"]?.progress?.phase)
    }
    @Test fun correctionCannotUseOfflineQueueWhileNewEventCan()=runTest {
        var calls=0;val vm=model(EventWriter {_,_->calls++},cached=true);runCurrent()
        submit(vm,"correction",old=MatchEvent(id="old",teamId="a"));runCurrent()
        assertEquals(SubmissionPhase.REJECTED,vm.state.value.eventWrites["correction"]?.progress?.phase);assertEquals(0,calls)
        submit(vm,"new");runCurrent();assertEquals(1,calls);assertEquals(SubmissionPhase.CONFIRMED,vm.state.value.eventWrites["new"]?.progress?.phase)
    }
}
