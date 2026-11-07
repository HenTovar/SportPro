package pe.edu.esan.sportpro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.*
import pe.edu.esan.sportpro.data.repository.*

data class CommunityUiState(val loading:Boolean=true,val profile: User? = null,val posts: List<CommunityPost> = emptyList(),
    val thread: CommunityThread=CommunityThread(),val selected: CommunityPost?=null,
    val error: String?=null,val reports: List<CommunityReport>?=null)
class CommunityViewModel(private val team: String,private val repo: CommunityRepository,private val auth: AuthRepository):ViewModel() {
    private val _state=MutableStateFlow(CommunityUiState());val state=_state.asStateFlow();private var threadJob:Job?=null
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
    fun post(text:String)=action { repo.post(team,text) }
    fun edit(post:CommunityPost,text:String)=action { repo.edit(team,post.id,text) }
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
    fun comment(text:String)=action { repo.comment(team,requireNotNull(_state.value.selected).id,text) }
    fun hideComment(comment:CommunityComment)=action { repo.hideComment(team,requireNotNull(_state.value.selected).id,comment.id) }
    fun react()=action { repo.react(team,requireNotNull(_state.value.selected).id,_state.value.thread.liked) }
    fun report(post:CommunityPost,reason:String)=action { repo.report(team,post.id,reason) }
    fun reports(post:CommunityPost)=action { val reports=repo.reports(team,post.id);_state.update { it.copy(reports=reports) } }
    fun closeReports() { _state.update { it.copy(reports=null) } }
}
