package pe.edu.esan.sportpro.ui.guided

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.GuidedProfile
import pe.edu.esan.sportpro.data.model.UserRole
import pe.edu.esan.sportpro.data.repository.GuidedAccounts
import pe.edu.esan.sportpro.data.repository.Result

data class GuidedAccountState(
    val draft: GuidedFormDraft = GuidedFormDraft(), val loaded: Boolean = false,
    val busy: Boolean = false, val message: String? = null,
    val errors: Map<ProfileField, String> = emptyMap(), val revision: Int = 0,
    val registered: Boolean = false, val coachPending: Boolean = false
)

/** No credentials or raw repository errors are retained after registration. */
class GuidedAccountViewModel(private val accounts: GuidedAccounts) : ViewModel() {
    private val mutable = MutableStateFlow(GuidedAccountState())
    val state = mutable.asStateFlow()
    private var original: GuidedProfile? = null
    fun change(draft: GuidedFormDraft) {
        if (!mutable.value.busy) mutable.update { it.copy(draft = draft, errors = emptyMap()) }
    }
    fun load() {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true, message = null, loaded = false) }
        viewModelScope.launch {
            try {
                val profile = accounts.loadMyData(); original = profile
                mutable.update { it.copy(draft = profile.asDraft(), loaded = true, coachPending = profile.coachPending) }
            } catch (e: CancellationException) { throw e }
              catch (e: Exception) { mutable.update { it.copy(message = "No se pudieron cargar tus datos. Vuelve a intentarlo.") } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
    fun register(draft: GuidedFormDraft, email: String, password: String) {
        if (mutable.value.busy || mutable.value.registered) return
        val errors = draft.errors(FormAccess.REGISTRATION)
        if (errors.isNotEmpty()) { mutable.update { it.copy(errors = errors) }; return }
        mutable.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                accounts.register(email, password, draft.asProfile()).collect { result ->
                    when (result) {
                        is Result.Success -> mutable.update { it.copy(registered = true,
                            coachPending = draft.role == UserRole.ENTRENADOR,
                            message = if (draft.role == UserRole.ENTRENADOR) "Pendiente de aprobación. Tus datos ya están guardados; no necesitas registrarlos de nuevo." else null) }
                        is Result.Error -> mutable.update { it.copy(message = "No se pudo crear la cuenta. Revisa los datos o vuelve a intentarlo.") }
                        else -> Unit
                    }
                }
            } catch (e: CancellationException) { throw e }
              catch (e: Exception) { mutable.update { it.copy(message = "No se pudo completar el registro.") } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
    fun save() {
        if (!mutable.value.loaded || mutable.value.busy) return
        val draft = mutable.value.draft
        val errors = draft.errors(FormAccess.SELF)
        if (errors.isNotEmpty()) { mutable.update { it.copy(errors = errors) }; return }
        perform("Datos guardados.") { accounts.saveMyData(draft.asProfile(original)) }
    }
    fun join(code: String) {
        if (!mutable.value.loaded || code.isBlank()) return
        if (original != mutable.value.draft.asProfile(original)) {
            mutable.update { it.copy(message = "Guarda tus datos antes de unirte al equipo.") }; return
        }
        perform("Te uniste al equipo. Tu ficha se creó con tus datos.") { accounts.joinTeam(code.trim()) }
    }
    fun leave(expectedTeamId: String) {
        if (!mutable.value.loaded || expectedTeamId.isBlank()) return
        perform("Te retiraste del equipo. Tu historial se conserva.") { accounts.leaveTeam(expectedTeamId) }
    }
    private fun perform(success: String, operation: suspend () -> Unit) {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                operation()
                original = mutable.value.draft.asProfile(original)
                mutable.update { it.copy(draft = checkNotNull(original).asDraft(), message = success, revision = it.revision + 1) }
            } catch (e: CancellationException) { throw e }
              catch (e: Exception) { mutable.update { it.copy(message = "No se confirmó el cambio. Revisa tu sesión y el estado del equipo antes de reintentar.") } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
}
