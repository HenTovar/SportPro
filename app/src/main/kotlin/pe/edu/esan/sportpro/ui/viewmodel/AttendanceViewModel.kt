package pe.edu.esan.sportpro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.AttendanceRecord
import pe.edu.esan.sportpro.data.model.AttendanceStatus
import pe.edu.esan.sportpro.data.model.Player
import pe.edu.esan.sportpro.data.model.Training
import pe.edu.esan.sportpro.data.model.User
import pe.edu.esan.sportpro.data.model.UserRole
import pe.edu.esan.sportpro.data.repository.AuthRepository
import pe.edu.esan.sportpro.data.repository.PlayerRepository
import pe.edu.esan.sportpro.data.repository.TrainingRepository
import pe.edu.esan.sportpro.data.repository.Result

/**
 * Estado de la pantalla de asistencia.
 */
data class AttendanceUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val players: List<Player> = emptyList(),
    val training: Training? = null,
    val attendanceMarks: Map<String, Boolean> = emptyMap(), // playerId -> isPresent
    val currentUser: User? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

/**
 * Estado para historial de asistencia de un jugador.
 */
data class AttendanceHistoryUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val records: List<AttendanceRecord> = emptyList(),
    val player: Player? = null,
    val attendancePercentage: Float = 0f
)

/**
 * ViewModel para gestionar asistencia en entrenamientos.
 */
class AttendanceViewModel(
    private val playerRepository: PlayerRepository,
    private val trainingRepository: TrainingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    /**
     * Carga los datos iniciales para marcar asistencia.
     */
    fun loadAttendanceData(teamId: String, trainingId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Obtener usuario actual para verificar permisos
            authRepository.getCurrentUser().collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.value = _uiState.value.copy(currentUser = result.data)
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(error = result.message)
                    }
                    is Result.Loading -> {}
                }
            }

            // Obtener jugadores del equipo
            playerRepository.getPlayersByTeam(teamId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.value = _uiState.value.copy(
                            players = result.data,
                            isLoading = false
                        )
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = result.message,
                            isLoading = false
                        )
                    }
                    is Result.Loading -> {}
                }
            }
        }
    }

    /**
     * Marca un jugador como presente o ausente.
     */
    fun toggleAttendance(playerId: String, isPresent: Boolean) {
        val current = _uiState.value.attendanceMarks.toMutableMap()
        current[playerId] = isPresent
        _uiState.value = _uiState.value.copy(attendanceMarks = current)
    }

    /**
     * Guarda todos los registros de asistencia.
     */
    fun saveAttendance(teamId: String, trainingId: String) {
        val currentUser = _uiState.value.currentUser
        val canMark = currentUser?.role in listOf(UserRole.ADMIN, UserRole.ENTRENADOR)

        if (!canMark) {
            _uiState.value = _uiState.value.copy(
                error = "Solo administradores y entrenadores pueden marcar asistencia"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isSaving = true)

        viewModelScope.launch {
            try {
                val attendanceMarks = _uiState.value.attendanceMarks
                for ((playerId, isPresent) in attendanceMarks) {
                    val record = AttendanceRecord(
                        playerId = playerId,
                        trainingId = trainingId,
                        status = if (isPresent) AttendanceStatus.ASISTIO else AttendanceStatus.FALTA,
                        recordedAt = System.currentTimeMillis()
                    )

                    trainingRepository.recordAttendance(teamId, record).collect { result ->
                        when (result) {
                            is Result.Success -> {
                                // Continue saving
                            }
                            is Result.Error -> {
                                _uiState.value = _uiState.value.copy(
                                    error = result.message,
                                    isSaving = false
                                )
                            }
                            is Result.Loading -> {}
                        }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = true,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Error al guardar asistencia",
                    isSaving = false
                )
            }
        }
    }

    /**
     * Verifica si el usuario actual puede marcar asistencia.
     */
    fun canMarkAttendance(): Boolean {
        val currentUser = _uiState.value.currentUser
        return currentUser?.role in listOf(UserRole.ADMIN, UserRole.ENTRENADOR)
    }
}

/**
 * ViewModel para historial de asistencia de un jugador.
 */
class PlayerAttendanceHistoryViewModel(
    private val trainingRepository: TrainingRepository,
    private val playerRepository: PlayerRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AttendanceHistoryUiState())
    val uiState: StateFlow<AttendanceHistoryUiState> = _uiState.asStateFlow()

    /**
     * Carga el historial de asistencia de un jugador.
     */
    fun loadAttendanceHistory(teamId: String, playerId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Obtener datos del jugador
            playerRepository.getPlayerById(teamId, playerId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.value = _uiState.value.copy(player = result.data)
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(error = result.message)
                    }
                    is Result.Loading -> {}
                }
            }

            // Obtener historial de asistencia
            trainingRepository.getAttendanceHistory(teamId, playerId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val records = result.data
                        val percentage = calculateAttendancePercentage(records)
                        _uiState.value = _uiState.value.copy(
                            records = records,
                            attendancePercentage = percentage,
                            isLoading = false
                        )
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = result.message,
                            isLoading = false
                        )
                    }
                    is Result.Loading -> {}
                }
            }
        }
    }

    /**
     * Calcula el porcentaje de asistencia basado en los registros.
     */
    private fun calculateAttendancePercentage(records: List<AttendanceRecord>): Float {
        if (records.isEmpty()) return 0f
        val presentCount = records.count { it.status == AttendanceStatus.ASISTIO }
        return (presentCount.toFloat() / records.size.toFloat()) * 100
    }
}
