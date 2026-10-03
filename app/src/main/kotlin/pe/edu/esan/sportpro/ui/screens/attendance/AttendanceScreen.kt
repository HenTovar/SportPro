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

    LaunchedEffect(Unit) {
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

            // Lista de jugadores con toggles
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.players) { player ->
                    AttendancePlayerItem(
                        player = player,
                        isPresent = uiState.attendanceMarks[player.id] ?: false,
                        onToggle = { isPresent ->
                            viewModel.toggleAttendance(player.id, isPresent)
                        },
                        canEdit = viewModel.canMarkAttendance()
                    )
                }
            }

            // Botón de guardar
            if (viewModel.canMarkAttendance()) {
                Button(
                    onClick = {
                        viewModel.saveAttendance(teamId, trainingId)
                    },
                    enabled = !uiState.isSaving && uiState.players.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (uiState.isSaving) "Guardando..." else "Guardar Asistencia"
                    )
                }

                if (uiState.saveSuccess) {
                    LaunchedEffect(Unit) {
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
    canEdit: Boolean
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
                Text(
                    if (isPresent) "Presente" else "Ausente",
                    style = MaterialTheme.typography.labelSmall
                )
            } else {
                Text(
                    if (isPresent) "Presente" else "Ausente",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
