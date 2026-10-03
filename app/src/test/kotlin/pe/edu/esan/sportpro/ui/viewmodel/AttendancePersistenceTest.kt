package pe.edu.esan.sportpro.ui.viewmodel

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import pe.edu.esan.sportpro.data.model.*
import pe.edu.esan.sportpro.data.repository.*
import pe.edu.esan.sportpro.data.repository.Result

@OptIn(ExperimentalCoroutinesApi::class)
class AttendancePersistenceTest {
    private val players: PlayerRepository = mock()
    private val trainings: TrainingRepository = mock()
    private val auth: AuthRepository = mock()
    private lateinit var vm: AttendanceViewModel

    @Before fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        whenever(auth.getCurrentUser()).thenReturn(flowOf(Result.Success(User(uid = "coach", role = UserRole.ENTRENADOR))))
        whenever(players.getPlayersByTeam("t")).thenReturn(flowOf(Result.Success(listOf(
            Player(id = "p1"), Player(id = "p2")
        ))))
        whenever(trainings.getTrainingsByTeam("t")).thenReturn(flowOf(Result.Success(listOf(Training(id = "s")))))
        vm = AttendanceViewModel(players, trainings, auth)
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun reloadAndEditPreservesIdAndUntouchedInjury() = runTest {
        val saved = AttendanceRecord(id = "existing", playerId = "p1", trainingId = "s", status = AttendanceStatus.ASISTIO, notes = "Keep")
        val injury = AttendanceRecord(id = "injury", playerId = "p2", trainingId = "s", status = AttendanceStatus.LESION)
        whenever(trainings.getAttendanceForTraining("t", "s")).thenReturn(flowOf(Result.Success(listOf(saved, injury))))
        whenever(trainings.recordAttendance(eq("t"), any())).thenAnswer { invocation ->
            flowOf(Result.Success(invocation.getArgument<AttendanceRecord>(1)))
        }
        vm.loadAttendanceData("t", "s")
        assertThat(vm.uiState.value.attendanceMarks["p1"]).isTrue()
        vm.toggleAttendance("p1", false)
        vm.saveAttendance("t", "s")
        val captured = argumentCaptor<AttendanceRecord>()
        verify(trainings).recordAttendance(eq("t"), captured.capture())
        assertThat(captured.firstValue.id).isEqualTo("existing")
        assertThat(captured.firstValue.notes).isEqualTo("Keep")
        assertThat(captured.firstValue.status).isEqualTo(AttendanceStatus.FALTA)
        assertThat(vm.uiState.value.savedRecords["p2"]?.status).isEqualTo(AttendanceStatus.LESION)
        assertThat(vm.uiState.value.saveSuccess).isTrue()
    }

    @Test fun failedReadDisablesSaving() = runTest {
        whenever(trainings.getAttendanceForTraining("t", "s")).thenReturn(flowOf(Result.Error("Sin conexión")))
        vm.loadAttendanceData("t", "s")
        vm.toggleAttendance("p1", false)
        vm.saveAttendance("t", "s")
        assertThat(vm.uiState.value.isReady).isFalse()
        verify(trainings, never()).recordAttendance(any(), any())
    }

    @Test fun writeFailureDoesNotReportSuccessOrLosePendingEdit() = runTest {
        whenever(trainings.getAttendanceForTraining("t", "s")).thenReturn(flowOf(Result.Success(emptyList())))
        whenever(trainings.recordAttendance(eq("t"), any())).thenReturn(flowOf(Result.Error("No guardado")))
        vm.loadAttendanceData("t", "s")
        vm.toggleAttendance("p1", true)
        vm.saveAttendance("t", "s")
        assertThat(vm.uiState.value.saveSuccess).isFalse()
        assertThat(vm.uiState.value.error).isEqualTo("No guardado")
        assertThat(vm.uiState.value.editedPlayers).contains("p1")
    }

    @Test fun reopeningWithoutChangesDoesNotWriteFalseDefaults() = runTest {
        whenever(trainings.getAttendanceForTraining("t", "s")).thenReturn(flowOf(Result.Success(emptyList())))
        vm.loadAttendanceData("t", "s")
        vm.saveAttendance("t", "s")
        verify(trainings, never()).recordAttendance(any(), any())
    }
}
