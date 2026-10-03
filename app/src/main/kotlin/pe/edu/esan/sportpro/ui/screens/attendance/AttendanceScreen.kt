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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import pe.edu.esan.sportpro.di.DefaultAppContainer
import pe.edu.esan.sportpro.ui.viewmodel.AttendanceViewModel

/**
 * Pantalla para marcar asistencia de jugadores en un entrenamiento.
 */
@Composable
fun AttendanceScreen(
    teamId: String,
    trainingId: String,
    navController: NavHostController
) {
    val container = remember { DefaultAppContainer() }
    val viewModel = remember {
        AttendanceViewModel(
            playerRepository = container.playerRepository,
            trainingRepository = container.trainingRepository,
            authRepository = container.authRepository
        )
    }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(teamId, trainingId) {
        viewModel.loadAttendanceData(teamId, trainingId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Marcar Asistencia") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Verificar permisos
            if (!viewModel.canMarkAttendance()) {
                Text(
                    "No tienes permisos para marcar asistencia",
                    color = MaterialTheme.colorScheme.error
                )
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

            uiState.training?.let { training ->
                Text(training.name, style = MaterialTheme.typography.titleMedium)
                Text(java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(training.date)))
            }
            Text("Marca presente con la casilla; pulsa el estado para marcar ausente.")
            // Lista de jugadores con toggles
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.players, key = { it.id }) { player ->
                    AttendancePlayerItem(
                        player = player,
                        isPresent = uiState.attendanceMarks[player.id] ?: false,
                        onToggle = { isPresent ->
                            viewModel.toggleAttendance(player.id, isPresent)
                        },
                        canEdit = viewModel.canMarkAttendance() && uiState.isReady && !uiState.isSaving,
                        statusLabel = if (player.id in uiState.editedPlayers) null else
                            uiState.savedRecords[player.id]?.status?.let { attendanceStatusLabel(it) } ?: "Sin registrar"
                    )
                }
            }

            // Botón de guardar
            if (viewModel.canMarkAttendance()) {
                Button(
                    onClick = {
                        viewModel.saveAttendance(teamId, trainingId)
                    },
                    enabled = uiState.isReady && !uiState.isSaving && uiState.editedPlayers.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (uiState.isSaving) "Guardando..." else "Guardar Asistencia"
                    )
                }

                if (uiState.saveSuccess) {
                    LaunchedEffect(teamId, trainingId) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}

/**
 * Elemento de lista para un jugador con toggle de asistencia.
 */
@Composable
fun AttendancePlayerItem(
    player: pe.edu.esan.sportpro.data.model.Player,
    isPresent: Boolean,
    onToggle: (Boolean) -> Unit,
    canEdit: Boolean,
    statusLabel: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = player.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )

            if (canEdit) {
                Checkbox(
                    checked = isPresent,
                    onCheckedChange = { onToggle(it) },
                    modifier = Modifier.size(24.dp)
                )
                TextButton(onClick = { onToggle(false) }) {
                    Text(statusLabel ?: if (isPresent) "Presente" else "Ausente")
                }
            } else {
                Text(
                    statusLabel ?: if (isPresent) "Presente" else "Ausente",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
