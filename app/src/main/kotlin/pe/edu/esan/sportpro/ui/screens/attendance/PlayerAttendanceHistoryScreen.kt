@file:Suppress("EXPERIMENTAL_MATERIAL3_API")

package pe.edu.esan.sportpro.ui.screens.attendance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import pe.edu.esan.sportpro.data.model.AttendanceStatus
import pe.edu.esan.sportpro.di.DefaultAppContainer
import pe.edu.esan.sportpro.ui.viewmodel.PlayerAttendanceHistoryViewModel

/**
 * Pantalla que muestra el historial de asistencia de un jugador.
 */
@Composable
fun PlayerAttendanceHistoryScreen(
    teamId: String,
    playerId: String,
    navController: NavHostController
) {
    val container = remember { DefaultAppContainer() }
    val viewModel = remember {
        PlayerAttendanceHistoryViewModel(
            trainingRepository = container.trainingRepository,
            playerRepository = container.playerRepository
        )
    }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(teamId, playerId) {
        viewModel.loadAttendanceHistory(teamId, playerId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Asistencia") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Información del jugador
            uiState.player?.let { player ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Posición: ${player.position}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Porcentaje de asistencia
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Porcentaje de Asistencia",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = String.format("%.1f%%", uiState.attendancePercentage),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            if (uiState.error != null) {
                Text(
                    uiState.error ?: "Error desconocido",
                    color = MaterialTheme.colorScheme.error
                )
            }

            AttendanceCalendar(uiState.trainings, uiState.records)

            Text("Entrenamientos programados", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.trainings.isEmpty()) {
                    item { Text("No hay entrenamientos programados") }
                }
                items(uiState.trainings, key = { it.id }) { training ->
                    val record = uiState.records.find { it.trainingId == training.id }
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(training.name)
                                Text(java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                                    .format(java.util.Date(training.date)))
                            }
                            Text(if (training.status == pe.edu.esan.sportpro.data.model.TrainingStatus.CANCELADO)
                                "Cancelado" else record?.status?.let { attendanceStatusLabel(it) } ?: "Sin registrar")
                        }
                    }
                }
                // Keep historical marks visible if their training is no longer available.
                items(uiState.records.filter { record -> uiState.trainings.none { it.id == record.trainingId } }) { record ->
                    AttendanceRecordItem(record)
                }
            }
        }
    }
}

/**
 * Elemento de lista para un registro de asistencia.
 */
@Composable
fun AttendanceRecordItem(
    record: pe.edu.esan.sportpro.data.model.AttendanceRecord
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Sesión: ${record.trainingId.take(8)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = java.text.SimpleDateFormat("dd/MM/yyyy").format(
                        java.util.Date(record.recordedAt)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Text(
                text = when (record.status) {
                    AttendanceStatus.ASISTIO -> "✓ Presente"
                    AttendanceStatus.FALTA -> "✗ Ausente"
                    AttendanceStatus.LESION -> "⚠ Lesión"
                    AttendanceStatus.EXCUSED -> "~ Excusado"
                },
                style = MaterialTheme.typography.bodySmall,
                color = when (record.status) {
                    AttendanceStatus.ASISTIO -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
}


internal fun attendanceStatusLabel(status: AttendanceStatus): String = when (status) {
    AttendanceStatus.ASISTIO -> "✓ Presente"
    AttendanceStatus.FALTA -> "✗ Ausente"
    AttendanceStatus.LESION -> "Lesión"
    AttendanceStatus.EXCUSED -> "Excusado"
}

/** Calendar dates come from the scheduled session, never the time a mark was edited. */
@Composable
private fun AttendanceCalendar(
    trainings: List<pe.edu.esan.sportpro.data.model.Training>,
    records: List<pe.edu.esan.sportpro.data.model.AttendanceRecord>
) {
    var monthOffset by remember { mutableStateOf(0) }
    val month = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.DAY_OF_MONTH, 1)
        add(java.util.Calendar.MONTH, monthOffset)
    }
    val year = month.get(java.util.Calendar.YEAR)
    val monthNumber = month.get(java.util.Calendar.MONTH)
    val sessions = trainings.filter { training ->
        java.util.Calendar.getInstance().apply { timeInMillis = training.date }.let {
            it.get(java.util.Calendar.YEAR) == year && it.get(java.util.Calendar.MONTH) == monthNumber
        }
    }.groupBy { training ->
        java.util.Calendar.getInstance().apply { timeInMillis = training.date }.get(java.util.Calendar.DAY_OF_MONTH)
    }
    val marks = records.associateBy { it.trainingId }
    val offset = (month.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
    val days = month.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { monthOffset-- }) { Text("‹") }
            Text(java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(month.time))
            TextButton(onClick = { monthOffset++ }) { Text("›") }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("L", "M", "M", "J", "V", "S", "D").forEach { day ->
                Text(day, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        repeat((offset + days + 6) / 7) { week ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { weekday ->
                    val day = week * 7 + weekday - offset + 1
                    val daySessions = sessions[day].orEmpty().filter {
                        it.status != pe.edu.esan.sportpro.data.model.TrainingStatus.CANCELADO
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (day in 1..days) day.toString() else "")
                        Text(daySessions.joinToString(" ") { session ->
                            when (marks[session.id]?.status) {
                                AttendanceStatus.ASISTIO -> "✓"
                                AttendanceStatus.FALTA -> "✗"
                                AttendanceStatus.LESION -> "L"
                                AttendanceStatus.EXCUSED -> "E"
                                null -> "·"
                            }
                        }, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Text("✓ Presente · ✗ Ausente · L Lesión · E Excusado · · Sin registrar",
            style = MaterialTheme.typography.labelSmall)
    }
}
