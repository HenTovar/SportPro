@file:Suppress("EXPERIMENTAL_MATERIAL3_API")

package pe.edu.esan.sportpro.ui.screens.trainings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import pe.edu.esan.sportpro.data.model.Training
import pe.edu.esan.sportpro.di.DefaultAppContainer
import pe.edu.esan.sportpro.ui.common.ContenidoLista
import pe.edu.esan.sportpro.ui.common.EstadoLista
import pe.edu.esan.sportpro.ui.common.aEstadoLista
import pe.edu.esan.sportpro.ui.common.rememberEsStaff
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sesiones de entrenamiento de un equipo, de la más reciente a la más antigua.
 * Desde cada sesión se registra la asistencia. Sólo el staff crea sesiones.
 */
@Composable
fun TrainingsScreen(teamId: String, navController: NavHostController) {
    val container = remember { DefaultAppContainer() }
    val esStaff = rememberEsStaff(container.authRepository)
    var estado by remember { mutableStateOf<EstadoLista<Training>>(EstadoLista.Cargando) }
    val formatoFecha = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE")) }

    LaunchedEffect(teamId) {
        container.trainingRepository.getTrainingsByTeam(teamId).collect { estado = it.aEstadoLista() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entrenamientos") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            if (esStaff) {
                FloatingActionButton(onClick = { navController.navigate("createTraining/$teamId") }) {
                    Icon(Icons.Filled.Add, contentDescription = "Planificar entrenamiento")
                }
            }
        }
    ) { padding ->
        ContenidoLista(
            estado = estado,
            textoVacio = "No hay entrenamientos planificados.",
            modifier = Modifier.padding(padding)
        ) { sesiones ->
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sesiones.sortedByDescending { it.date }) { sesion ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(sesion.name.ifBlank { "Entrenamiento" }, style = MaterialTheme.typography.titleMedium)
                            Text("${formatoFecha.format(Date(sesion.date))} · ${sesion.duration} min")
                            if (sesion.objective.isNotBlank()) Text("Objetivo: ${sesion.objective}")
                            if (sesion.exercises.isNotEmpty()) {
                                Text(
                                    "Ejercicios: ${sesion.exercises.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            OutlinedButton(
                                onClick = { navController.navigate("attendance/$teamId/${sesion.id}") },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text(if (esStaff) "Registrar asistencia" else "Ver asistencia")
                            }
                        }
                    }
                }
            }
        }
    }
}
