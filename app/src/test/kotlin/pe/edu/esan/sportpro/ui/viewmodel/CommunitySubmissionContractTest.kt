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
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CommunitySubmissionContractTest {
    private val instances=mutableListOf<ViewModel>()
    private val first=CommunityPost("first","Primera","owner","VISIBLE",1)
    private val second=CommunityPost("second","Segunda","owner","VISIBLE",2)
    @Before fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun cleanup() { instances.forEach { it.viewModelScope.cancel() };Dispatchers.resetMain() }
    private fun model(writer: CommunityWriter):CommunityViewModel {
        val repo:CommunityRepository=mock();val auth:AuthRepository=mock()
        whenever(auth.getCurrentUser()).thenReturn(flowOf(Result.Success(User(uid="coach",role=UserRole.ENTRENADOR,coachTeamIds=listOf("team")))))
        whenever(repo.watch(eq("team"),any())).thenReturn(flowOf(Result.Success(listOf(first,second))))
        whenever(repo.thread(eq("team"),any(),any())).thenReturn(flowOf(Result.Success(CommunityThread())))
        return CommunityViewModel("team",repo,auth,writer).also {instances.add(it)}
    }
    @Test fun pendingPostRetainsTextAndDoubleTapWritesOnce()=runTest {
        val done=CompletableDeferred<Unit>();var calls=0
        val vm=model(CommunityWriter {calls++;done.await()});runCurrent()
        vm.post("Borrador");vm.post("Segundo toque");runCurrent()
        assertEquals(1,calls);assertEquals("Borrador",vm.state.value.composer.text)
        assertEquals(SubmissionPhase.SAVING,vm.state.value.composer.progress.phase)
        vm.setPostText("No debe reemplazar");assertEquals("Borrador",vm.state.value.composer.text)
        done.complete(Unit);runCurrent()
        assertEquals("",vm.state.value.composer.text);assertEquals(SubmissionPhase.CONFIRMED,vm.state.value.composer.progress.phase)
    }
    @Test fun rejectedPostKeepsTextAndErrorAndAllowsExplicitRetry()=runTest {
        var calls=0;val vm=model(CommunityWriter {if(++calls==1)throw IllegalArgumentException("Denegado")});runCurrent()
        vm.post("Conservar");runCurrent()
        assertEquals("Conservar",vm.state.value.composer.text);assertEquals(SubmissionPhase.REJECTED,vm.state.value.composer.progress.phase)
        assertEquals("Denegado",vm.state.value.composer.progress.message)
        vm.publish();runCurrent();assertEquals(2,calls);assertEquals("",vm.state.value.composer.text)
    }
    @Test fun editingFailureRetainsPostIdentityAndEditedText()=runTest {
        val requests=mutableListOf<CommunityWriteRequest>();val vm=model(CommunityWriter {requests.add(it);throw IllegalArgumentException("No autorizado")});runCurrent()
        vm.beginEdit(first);vm.setPostText("Texto editado");vm.publish();runCurrent()
        assertEquals(CommunityWriteKind.EDIT,requests.single().kind);assertEquals(first.id,requests.single().postId)
        assertEquals(first.id,vm.state.value.composer.postId);assertEquals("Texto editado",vm.state.value.composer.text)
        assertEquals(SubmissionPhase.REJECTED,vm.state.value.composer.progress.phase)
    }
    @Test fun pendingCommentIsRetainedWhenClosingAndReopeningThread()=runTest {
        val done=CompletableDeferred<Unit>();var calls=0
        val vm=model(CommunityWriter {calls++;done.await()});runCurrent();vm.open(first);runCurrent()
        vm.comment("Mi comentario");vm.comment("Doble");runCurrent();vm.close();vm.open(first);runCurrent()
        assertEquals(1,calls);assertEquals("Mi comentario",vm.state.value.comments[first.id]?.text)
        assertEquals(SubmissionPhase.SAVING,vm.state.value.comments[first.id]?.progress?.phase)
        done.complete(Unit);runCurrent();assertEquals("",vm.state.value.comments[first.id]?.text)
    }
    @Test fun rejectedCommentRetainsTextInItsOwnThread()=runTest {
        val vm=model(CommunityWriter {throw IllegalArgumentException("Comentario rechazado")});runCurrent();vm.open(first);runCurrent()
        vm.comment("No perder");runCurrent();assertEquals("No perder",vm.state.value.comments[first.id]?.text)
        assertEquals(SubmissionPhase.REJECTED,vm.state.value.comments[first.id]?.progress?.phase)
        assertEquals("Comentario rechazado",vm.state.value.comments[first.id]?.progress?.message)
    }
    @Test fun lateCommentSuccessDoesNotEraseAnotherThreadDraft()=runTest {
        val done=CompletableDeferred<Unit>();val vm=model(CommunityWriter {done.await()});runCurrent();vm.open(first);runCurrent()
        vm.comment("Enviado al primero");runCurrent();vm.close();vm.open(second);runCurrent();vm.setCommentText(second.id,"Borrador del segundo")
        done.complete(Unit);runCurrent()
        assertEquals("",vm.state.value.comments[first.id]?.text);assertEquals("Borrador del segundo",vm.state.value.comments[second.id]?.text)
        assertEquals(second.id,vm.state.value.selected?.id)
    }
    @Test fun longWaitBecomesUncertainWithoutResendAndLateSuccessCanResolve()=runTest {
        val done=CompletableDeferred<Unit>();var calls=0;val vm=model(CommunityWriter {calls++;done.await()});runCurrent()
        vm.post("Aún aquí");runCurrent();advanceTimeBy(10_000);runCurrent()
        assertEquals(SubmissionPhase.UNCERTAIN,vm.state.value.composer.progress.phase);assertEquals("Aún aquí",vm.state.value.composer.text)
        vm.publish();runCurrent();assertEquals(1,calls)
        done.complete(Unit);runCurrent();assertEquals(SubmissionPhase.CONFIRMED,vm.state.value.composer.progress.phase);assertEquals("",vm.state.value.composer.text)
    }
    @Test fun transportFailureIsUncertainAndDoesNotClearOrRetry()=runTest {
        var calls=0;val vm=model(CommunityWriter {calls++;throw IOException("Connection lost")});runCurrent()
        vm.post("Conservar incertidumbre");runCurrent();assertEquals(SubmissionPhase.UNCERTAIN,vm.state.value.composer.progress.phase)
        assertEquals("Conservar incertidumbre",vm.state.value.composer.text);assertTrue(vm.state.value.composer.progress.blocksSend)
        vm.publish();runCurrent();assertEquals(1,calls)
    }
    @Test fun cancelledAcknowledgmentPreservesTextAsUncertain()=runTest {
        val vm=model(CommunityWriter {throw CancellationException("Task cancelled")});runCurrent()
        vm.post("Conservar cancelación");runCurrent()
        assertEquals("Conservar cancelación",vm.state.value.composer.text)
        assertEquals(SubmissionPhase.UNCERTAIN,vm.state.value.composer.progress.phase)
    }
    @Test fun postAndCommentHaveIndependentResults()=runTest {
        val postDone=CompletableDeferred<Unit>();val commentDone=CompletableDeferred<Unit>()
        val vm=model(CommunityWriter {if(it.kind==CommunityWriteKind.COMMENT)commentDone.await() else postDone.await()});runCurrent()
        vm.post("Publicación");vm.open(first);runCurrent();vm.comment("Comentario");runCurrent()
        commentDone.complete(Unit);runCurrent()
        assertEquals("Publicación",vm.state.value.composer.text);assertEquals(SubmissionPhase.SAVING,vm.state.value.composer.progress.phase)
        assertEquals("",vm.state.value.comments[first.id]?.text);postDone.complete(Unit);runCurrent()
        assertEquals("",vm.state.value.composer.text)
    }
}
