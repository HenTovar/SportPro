package pe.edu.esan.sportpro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.*
import pe.edu.esan.sportpro.data.repository.*
import pe.edu.esan.sportpro.data.network.NarrativeGenerator

data class LiveMatchUiState(val profile: User? = null,val feed: MatchFeed = MatchFeed(),
    val narrative: Narrative = Narrative(),val busy: Boolean = false,val error: String? = null,
    val history: List<EventRevision>? = null)
class LiveMatchViewModel(private val id: String,private val repo: MatchRepository,
    private val auth: AuthRepository,private val generator: NarrativeGenerator= NarrativeGenerator()):ViewModel() {
    private val _state=MutableStateFlow(LiveMatchUiState());val state=_state.asStateFlow()
    init {
        viewModelScope.launch { auth.getCurrentUser().collect { r -> when(r) {
            is Result.Success -> _state.update { it.copy(profile=r.data) }
            is Result.Error -> error(r.message)
            else -> Unit
        } } }
        viewModelScope.launch { repo.feed(id).collect { r -> when(r) {
            is Result.Success -> _state.update { it.copy(feed=r.data) }
            is Result.Error -> error(r.message)
            else -> Unit
        } } }
        viewModelScope.launch {
            val profile=auth.getCurrentUser().first { it !is Result.Loading }
            val user=(profile as? Result.Success)?.data
            if(user!=null)repo.narrative(id,RolePolicy.isStaff(user.role)).collect { r -> when(r) {
                is Result.Success -> _state.update { it.copy(narrative=r.data) }
                is Result.Error -> error(r.message)
                else -> Unit
            } }
        }
    }
    private fun error(message: String) { _state.update { it.copy(error=message) } }
    fun clearError() { _state.update { it.copy(error=null) } }
    private fun staff() { require(RolePolicy.isStaff(_state.value.profile?.role)) { "Sólo el entrenador o administrador puede registrar" } }
    private fun action(busy: Boolean=true,work: suspend ()->Unit) { viewModelScope.launch {
        if(busy)_state.update { it.copy(busy=true) }
        try { staff();work() } catch(e:Exception) { if(e is CancellationException)throw e;error(e.message ?: "No se pudo completar") }
        finally { if(busy)_state.update { it.copy(busy=false) } }
    } }
    fun setStatus(status: String)=action { repo.status(id,status) }
    fun saveType(type: EventType)=action { repo.type(id,type) }
    fun saveEvent(old: MatchEvent?,type: EventType,minute: String,team: String,player: String,note: String,voided: Boolean=false)=action(old!=null) {
        val match=requireNotNull(_state.value.feed.match);val time=MatchFacts.validate(minute,team,match,note,player)
        require(type.id.isNotBlank()) { "Selecciona un tipo del catálogo" }
        if(old==null) { require(type.active) { "Tipo desactivado" };repo.add(id,type,time,team,player,note) }
        else { require(!_state.value.feed.cached&&!_state.value.feed.pending) { "Sincroniza antes de corregir; no se borrará el evento" };repo.correct(id,old,type,time,team,player,note,voided) }
    }
    fun history(event: MatchEvent)=viewModelScope.launch {
        try { val history=repo.history(id,event.id);_state.update { it.copy(history=history) } }
        catch(e:Exception) { if(e is CancellationException)throw e;error("No se pudo abrir el historial") }
    }
    fun closeHistory() { _state.update { it.copy(history=null) } }
    fun generate()=action {
        require(!_state.value.feed.cached&&!_state.value.feed.pending) { "Conecta y sincroniza el partido antes de pedir el resumen" }
        val (match,events)=repo.confirmedFacts(id)
        require(events.any { !it.voided }) { "Registra al menos un evento vigente" }
        val text=generator.generate(match,events)
        repo.saveNarrative(id,text,MatchFacts.hash(events),match.eventVersion,false)
    }
    fun saveNarrative(text: String,approve: Boolean)=action {
        val current=_state.value;val draft=current.narrative
        require(draft.sourceHash.isNotBlank()) { "Primero genera un borrador con IA" }
        if(approve)require(!current.feed.cached&&!current.feed.pending&&draft.sourceHash==MatchFacts.hash(current.feed.events)
            && draft.sourceVersion==current.feed.match?.eventVersion) { "Los eventos cambiaron; genera y revisa otro borrador" }
        repo.saveNarrative(id,text,draft.sourceHash,draft.sourceVersion,approve)
    }
}
