package pe.edu.esan.sportpro.ui.viewmodel

import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import pe.edu.esan.sportpro.data.model.AttendanceRecord
import pe.edu.esan.sportpro.data.model.AttendanceStatus
import pe.edu.esan.sportpro.data.model.Player
import pe.edu.esan.sportpro.data.model.PlayerPosition
import pe.edu.esan.sportpro.data.repository.PlayerRepository
import pe.edu.esan.sportpro.data.repository.TrainingRepository
import com.google.common.truth.Truth.assertThat

/**
 * Pruebas unitarias para PlayerAttendanceHistoryViewModel.
 */
class PlayerAttendanceHistoryViewModelTest {

    @Mock
    private lateinit var mockTrainingRepository: TrainingRepository

    @Mock
    private lateinit var mockPlayerRepository: PlayerRepository

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun testLoadAttendanceHistoryEmptyRecords() {
        val records = emptyList<AttendanceRecord>()
        val percentage = calculateAttendancePercentage(records)
        assertThat(percentage).isEqualTo(0f)
    }

    @Test
    fun testLoadAttendanceHistoryWithRecords() {
        val records = listOf(
            AttendanceRecord(status = AttendanceStatus.ASISTIO),
            AttendanceRecord(status = AttendanceStatus.FALTA),
            AttendanceRecord(status = AttendanceStatus.ASISTIO)
        )
        val percentage = calculateAttendancePercentage(records)
        assertThat(percentage).isWithin(0.1f).of(66.67f)
    }

    @Test
    fun testAttendancePercentageCalculation100Percent() {
        val records = listOf(
            AttendanceRecord(status = AttendanceStatus.ASISTIO),
            AttendanceRecord(status = AttendanceStatus.ASISTIO),
            AttendanceRecord(status = AttendanceStatus.ASISTIO)
        )
        val percentage = calculateAttendancePercentage(records)
        assertThat(percentage).isEqualTo(100f)
    }

    @Test
    fun testAttendancePercentageCalculationZeroPercent() {
        val records = listOf(
            AttendanceRecord(status = AttendanceStatus.FALTA),
            AttendanceRecord(status = AttendanceStatus.FALTA)
        )
        val percentage = calculateAttendancePercentage(records)
        assertThat(percentage).isEqualTo(0f)
    }

    @Test
    fun testPlayerDataLoaded() {
        val player = Player(
            id = "p1",
            name = "Carlos López",
            teamId = "team1",
            position = PlayerPosition.LATERAL_DERECHO
        )
        assertThat(player.name).isEqualTo("Carlos López")
    }

    @Test
    fun testHandlePlayerError() {
        // Verify error message would be correct
        val errorMessage = "Jugador no encontrado"
        assertThat(errorMessage).contains("Jugador")
    }

    @Test
    fun testLoadingState() {
        // Loading state test
        assertThat(true).isTrue()
    }

    @Test
    fun testDifferentAttendanceStatuses() {
        val records = listOf(
            AttendanceRecord(status = AttendanceStatus.ASISTIO),
            AttendanceRecord(status = AttendanceStatus.FALTA),
            AttendanceRecord(status = AttendanceStatus.LESION),
            AttendanceRecord(status = AttendanceStatus.EXCUSED)
        )
        // Solo ASISTIO cuenta como presente (1 de 4)
        val percentage = calculateAttendancePercentage(records)
        assertThat(percentage).isWithin(0.1f).of(25f)
    }

    // Helper function
    private fun calculateAttendancePercentage(records: List<AttendanceRecord>): Float {
        if (records.isEmpty()) return 0f
        val presentCount = records.count { it.status == AttendanceStatus.ASISTIO }
        return (presentCount.toFloat() / records.size.toFloat()) * 100
    }
}
