package pe.edu.esan.sportpro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.AttendanceRecord
import pe.edu.esan.sportpro.data.model.AttendanceStatus
import pe.edu.esan.sportpro.data.model.Player
import pe.edu.esan.sportpro.data.model.TrainingStatus
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
    val savedRecords: Map<String, AttendanceRecord> = emptyMap(),
    val editedPlayers: Set<String> = emptySet(),
    val isReady: Boolean = false,
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
    val trainings: List<Training> = emptyList(),
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

    private var loadJob: Job? = null
    private var loadedSession: Pair<String, String>? = null

    /** Load all persisted marks before enabling edits; a failed read must never reset them. */
    fun loadAttendanceData(teamId: String, trainingId: String) {
        loadJob?.cancel()
        loadedSession = teamId to trainingId
        _uiState.value = AttendanceUiState(isLoading = true)
        loadJob = viewModelScope.launch {
            var loadError: String? = null
            authRepository.getCurrentUser().collect { result ->
                when (result) {
                    is Result.Success -> _uiState.value = _uiState.value.copy(currentUser = result.data)
                    is Result.Error -> loadError = result.message
                    is Result.Loading -> Unit
                }
            }
            playerRepository.getPlayersByTeam(teamId).collect { result ->
                when (result) {
                    is Result.Success -> _uiState.value = _uiState.value.copy(players = result.data.filter { it.isActive })
                    is Result.Error -> loadError = result.message
                    is Result.Loading -> Unit
                }
            }
            trainingRepository.getTrainingsByTeam(teamId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val training = result.data.find { it.id == trainingId }
                        _uiState.value = _uiState.value.copy(training = training)
                        if (training == null) loadError = "Entrenamiento no encontrado"
                        else if (training.status == TrainingStatus.CANCELADO) loadError = "El entrenamiento está cancelado"
                    }
                    is Result.Error -> loadError = result.message
                    is Result.Loading -> Unit
                }
            }
            trainingRepository.getAttendanceForTraining(teamId, trainingId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val records = result.data.sortedByDescending { it.recordedAt }.distinctBy { it.playerId }
                        _uiState.value = _uiState.value.copy(
                            savedRecords = records.associateBy { it.playerId },
                            attendanceMarks = records.associate { it.playerId to (it.status == AttendanceStatus.ASISTIO) }
                        )
                    }
                    is Result.Error -> loadError = result.message
                    is Result.Loading -> Unit
                }
            }
            _uiState.value = _uiState.value.copy(isLoading = false, error = loadError, isReady = loadError == null)
        }
    }

    /**
     * Marca un jugador como presente o ausente.
     */
    fun toggleAttendance(playerId: String, isPresent: Boolean) {
        if (_uiState.value.isLoading || _uiState.value.isSaving) return
        val current = _uiState.value.attendanceMarks.toMutableMap()
        current[playerId] = isPresent
        _uiState.value = _uiState.value.copy(
            attendanceMarks = current, editedPlayers = _uiState.value.editedPlayers + playerId, saveSuccess = false
        )
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

        val state = _uiState.value
        if (state.isSaving || !state.isReady || loadedSession != (teamId to trainingId)) return
        val changedPlayers = state.editedPlayers.filter { id -> state.players.any { it.id == id } }
        if (changedPlayers.isEmpty()) return
        _uiState.value = state.copy(isSaving = true, saveSuccess = false, error = null)
        viewModelScope.launch {
            try {
                var saveError: String? = null
                for (playerId in changedPlayers) {
                    val record = (state.savedRecords[playerId] ?: AttendanceRecord(
                        playerId = playerId, trainingId = trainingId
                    )).copy(status = if (state.attendanceMarks[playerId] == true) AttendanceStatus.ASISTIO else AttendanceStatus.FALTA)
                    trainingRepository.recordAttendance(teamId, record).collect { result ->
                        when (result) {
                            is Result.Success -> _uiState.value = _uiState.value.copy(
                                savedRecords = _uiState.value.savedRecords + (playerId to result.data),
                                editedPlayers = _uiState.value.editedPlayers - playerId
                            )
                            is Result.Error -> saveError = result.message
                            is Result.Loading -> Unit
                        }
                    }
                    if (saveError != null) break
                }
                _uiState.value = _uiState.value.copy(
                    isSaving = false, saveSuccess = saveError == null, error = saveError
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Error al guardar asistencia", isSaving = false, saveSuccess = false
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
    private var loadJob: Job? = null

    fun loadAttendanceHistory(teamId: String, playerId: String) {
        loadJob?.cancel()
        _uiState.value = AttendanceHistoryUiState(isLoading = true)
        loadJob = viewModelScope.launch {

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

            trainingRepository.getTrainingsByTeam(teamId).collect { result ->
                when (result) {
                    is Result.Success -> _uiState.value = _uiState.value.copy(
                        trainings = result.data.sortedBy { it.date }
                    )
                    is Result.Error -> _uiState.value = _uiState.value.copy(error = result.message)
                    is Result.Loading -> Unit
                }
            }

            // Obtener historial de asistencia
            trainingRepository.getAttendanceHistory(teamId, playerId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val records = result.data.sortedByDescending { it.recordedAt }.distinctBy { it.trainingId }
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
