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

    LaunchedEffect(Unit) {
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

            // Historial de registros
            Text(
                text = "Registros de Asistencia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.records.isEmpty()) {
                    item {
                        Text(
                            text = "Sin registros de asistencia",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    items(uiState.records) { record ->
                        AttendanceRecordItem(record)
                    }
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
                    text = "Sesión: ${record.trainingId.substring(0, 8)}",
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
