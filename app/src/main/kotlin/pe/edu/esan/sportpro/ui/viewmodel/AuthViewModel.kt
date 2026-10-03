package pe.edu.esan.sportpro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.User
import pe.edu.esan.sportpro.data.model.UserRole
import pe.edu.esan.sportpro.data.repository.AuthRepository
import pe.edu.esan.sportpro.data.repository.Result

/**
 * Estado de la interfaz de autenticación.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val selectedRole: UserRole = UserRole.JUGADOR,
    val loginSuccess: Boolean = false,
    val registerSuccess: Boolean = false
)

/**
 * ViewModel para manejar autenticación.
 */
class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Actualiza el email en la UI.
     */
    fun setEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email, error = null)
    }

    /**
     * Actualiza la contraseña en la UI.
     */
    fun setPassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    /**
     * Actualiza el nombre en la UI.
     */
    fun setName(name: String) {
        _uiState.value = _uiState.value.copy(name = name, error = null)
    }

    /**
     * Actualiza el rol seleccionado en la UI.
     */
    fun setRole(role: UserRole) {
        _uiState.value = _uiState.value.copy(selectedRole = role)
    }

    /**
     * Inicia sesión con email y contraseña.
     */
    fun login() {
        val state = _uiState.value

        // Validación
        if (state.email.isEmpty() || state.password.isEmpty()) {
            _uiState.value = state.copy(error = "Email y contraseña son requeridos")
            return
        }

        if (!authRepository.isEmailValid(state.email)) {
            _uiState.value = state.copy(error = "Email inválido")
            return
        }

        _uiState.value = state.copy(isLoading = true)

        viewModelScope.launch {
            authRepository.login(state.email, state.password).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _uiState.value = state.copy(isLoading = true)
                    }
                    is Result.Success -> {
                        _uiState.value = state.copy(
                            isLoading = false,
                            user = result.data,
                            loginSuccess = true,
                            error = null
                        )
                    }
                    is Result.Error -> {
                        _uiState.value = state.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Registra un nuevo usuario.
     */
    fun register() {
        val state = _uiState.value

        // Validación
        if (state.email.isEmpty() || state.password.isEmpty() || state.name.isEmpty()) {
            _uiState.value = state.copy(error = "Todos los campos son requeridos")
            return
        }

        if (!authRepository.isEmailValid(state.email)) {
            _uiState.value = state.copy(error = "Email inválido")
            return
        }

        if (!authRepository.isPasswordValid(state.password)) {
            _uiState.value = state.copy(error = "La contraseña debe tener al menos 6 caracteres")
            return
        }

        _uiState.value = state.copy(isLoading = true)

        viewModelScope.launch {
            authRepository.register(
                state.email,
                state.password,
                state.name,
                state.selectedRole
            ).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _uiState.value = state.copy(isLoading = true)
                    }
                    is Result.Success -> {
                        _uiState.value = state.copy(
                            isLoading = false,
                            user = result.data,
                            registerSuccess = true,
                            error = null
                        )
                    }
                    is Result.Error -> {
                        _uiState.value = state.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Limpia el estado de error.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Limpia el estado de éxito.
     */
    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(loginSuccess = false, registerSuccess = false)
    }
}
