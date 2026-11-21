package pe.edu.esan.sportpro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import pe.edu.esan.sportpro.data.model.*
import pe.edu.esan.sportpro.data.repository.*

data class CommunityUiState(val loading:Boolean=true,val profile: User? = null,val posts: List<CommunityPost> = emptyList(),
    val thread: CommunityThread=CommunityThread(),val selected: CommunityPost?=null,
    val error: String?=null,val reports: List<CommunityReport>?=null,
    val composer: TextSubmission=TextSubmission(), val comments: Map<String,TextSubmission> = emptyMap())
class CommunityViewModel(private val team: String,private val repo: CommunityRepository,private val auth: AuthRepository, private val writer: CommunityWriter? = null):ViewModel() {
    private val _state=MutableStateFlow(CommunityUiState());val state=_state.asStateFlow();private var threadJob:Job?=null
    private var nextSubmissionId=0L
    init { viewModelScope.launch {
        val profile=auth.getCurrentUser().first { it !is Result.Loading }
        if(profile !is Result.Success||profile.data==null||!RolePolicy.canReadTeam(profile.data,team)) { error("No tienes acceso a esta comunidad");return@launch }
        _state.update { it.copy(profile=profile.data) }
        repo.watch(team,RolePolicy.isStaff(profile.data.role)).collect { r -> when(r) {
            is Result.Success -> _state.update { current ->
                current.copy(loading=false,posts=r.data,selected=current.selected?.let { selected -> r.data.find { it.id==selected.id } })
            }
            is Result.Error -> error(r.message)
            else -> Unit
        } }
    } }
    private fun error(message:String) { _state.update { it.copy(error=message,loading=false) } }
    fun clearError() { _state.update { it.copy(error=null) } }
    private fun action(work:suspend ()->Unit)=viewModelScope.launch {
        try { require(_state.value.profile!=null) { "Inicia sesión" };work() }
        catch(e:Exception) { if(e is CancellationException)throw e;error(e.message ?: "No se pudo completar") }
    }
    fun setPostText(text: String) { _state.update { if(it.composer.progress.blocksSend) it else it.copy(composer=it.composer.copy(text=text)) } }
    fun beginEdit(post: CommunityPost) { _state.update { if(it.composer.progress.blocksSend) it else it.copy(composer=TextSubmission(text=post.text,postId=post.id)) } }
    fun cancelEdit() { _state.update { if(it.composer.progress.blocksSend) it else it.copy(composer=TextSubmission()) } }
    fun setCommentText(postId: String, text: String) { _state.update { current ->
        val draft=current.comments[postId] ?: TextSubmission()
        if(draft.progress.blocksSend) current else current.copy(comments=current.comments+(postId to draft.copy(text=text)))
    } }
    private fun submitText(commentPostId: String? = null) {
        val current=_state.value
        val draft=if(commentPostId==null) current.composer else current.comments[commentPostId] ?: TextSubmission()
        if(draft.progress.blocksSend) return
        val request=CommunityWriteRequest(if(commentPostId!=null) CommunityWriteKind.COMMENT else if(draft.postId!=null) CommunityWriteKind.EDIT else CommunityWriteKind.POST,
            draft.text, commentPostId ?: draft.postId)
        val id=++nextSubmissionId
        fun change(transform: (TextSubmission)->TextSubmission) { _state.update { state ->
            if(commentPostId==null) {
                if(state.composer.progress.id!=id) state else state.copy(composer=transform(state.composer))
            } else {
                val item=state.comments[commentPostId]
                if(item==null || item.progress.id!=id) state else state.copy(comments=state.comments+(commentPostId to transform(item)))
            }
        } }
        val saving=draft.copy(progress=SubmissionProgress(id,SubmissionPhase.SAVING))
        _state.update { if(commentPostId==null) it.copy(composer=saving) else it.copy(comments=it.comments+(commentPostId to saving)) }
        viewModelScope.launch {
            val watchdog=launch { delay(10_000);change { if(it.progress.phase==SubmissionPhase.SAVING) it.copy(progress=SubmissionProgress(id,SubmissionPhase.UNCERTAIN,uncertainWriteMessage())) else it } }
            try {
                val profile=_state.value.profile
                require(profile?.isActive==true && profile.uid.isNotBlank()) { "Inicia sesión con una cuenta activa" }
                require(request.text.isNotBlank() && request.text.length<=(if(request.kind==CommunityWriteKind.COMMENT)500 else 1000)) { "Revisa el texto y su longitud" }
                if(writer!=null) writer.write(request) else when(request.kind) {
                    CommunityWriteKind.POST -> repo.post(team,request.text)
                    CommunityWriteKind.EDIT -> repo.edit(team,requireNotNull(request.postId),request.text)
                    CommunityWriteKind.COMMENT -> repo.comment(team,requireNotNull(request.postId),request.text)
                }
                change { it.copy(text="",postId=null,progress=SubmissionProgress(id,SubmissionPhase.CONFIRMED)) }
            } catch(e:Exception) {
                if(e is CancellationException) {
                    change { it.copy(progress=SubmissionProgress(id,SubmissionPhase.UNCERTAIN,uncertainWriteMessage())) }
                    throw e
                }
                val phase=writeFailure(e)
                change { it.copy(progress=SubmissionProgress(id,phase,if(phase==SubmissionPhase.REJECTED)e.message ?: "No se pudo publicar" else uncertainWriteMessage())) }
            } finally { watchdog.cancel() }
        }
    }
    fun post(text:String) { if(_state.value.composer.progress.blocksSend)return;setPostText(text);submitText() }
    fun edit(post:CommunityPost,text:String) { if(_state.value.composer.progress.blocksSend)return;beginEdit(post);setPostText(text);submitText() }
    fun publish() = submitText()
    fun moderate(post:CommunityPost)=action { repo.moderate(team,post.id,post.state!="HIDDEN") }
    fun open(post:CommunityPost) {
        threadJob?.cancel();_state.update { it.copy(selected=post,thread=CommunityThread()) }
        threadJob=viewModelScope.launch { repo.thread(team,post.id,RolePolicy.isStaff(_state.value.profile?.role)).collect { r -> when(r) {
            is Result.Success -> _state.update { it.copy(thread=r.data) }
            is Result.Error -> error(r.message)
            else -> Unit
        } } }
    }
    fun close() { threadJob?.cancel();_state.update { it.copy(selected=null) } }
    fun comment(text:String) { _state.value.selected?.id?.let { comment(it,text) } }
    fun comment(postId:String,text:String) {
        if(_state.value.selected?.id!=postId || _state.value.comments[postId]?.progress?.blocksSend==true)return
        setCommentText(postId,text);submitText(postId)
    }
    fun hideComment(comment:CommunityComment)=action { repo.hideComment(team,requireNotNull(_state.value.selected).id,comment.id) }
    fun react()=action { repo.react(team,requireNotNull(_state.value.selected).id,_state.value.thread.liked) }
    fun report(post:CommunityPost,reason:String)=action { repo.report(team,post.id,reason) }
    fun reports(post:CommunityPost)=action { val reports=repo.reports(team,post.id);_state.update { it.copy(reports=reports) } }
    fun closeReports() { _state.update { it.copy(reports=null) } }
}
