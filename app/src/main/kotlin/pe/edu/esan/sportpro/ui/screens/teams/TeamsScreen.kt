@file:Suppress("EXPERIMENTAL_MATERIAL3_API")

package pe.edu.esan.sportpro.ui.screens.teams

import pe.edu.esan.sportpro.ui.navigation.safeBack

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import kotlinx.coroutines.flow.first
import pe.edu.esan.sportpro.data.model.RolePolicy
import pe.edu.esan.sportpro.data.repository.Result
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.Team
import pe.edu.esan.sportpro.di.DefaultAppContainer
import pe.edu.esan.sportpro.ui.common.ContenidoLista
import pe.edu.esan.sportpro.ui.common.EstadoLista
import pe.edu.esan.sportpro.ui.common.aEstadoLista
import pe.edu.esan.sportpro.ui.common.rememberEsStaff

/**
 * Lista de equipos por categoría. Desde cada equipo se entra a sus jugadores
 * y a sus entrenamientos. Sólo ADMIN/ENTRENADOR pueden crear o eliminar.
 */
@Composable
fun TeamsScreen(navController: NavHostController, pickTrainings: Boolean = false, pickMatches: Boolean = false) {
    val container = remember { DefaultAppContainer() }
    val esStaff = rememberEsStaff(container.authRepository)
    var estado by remember { mutableStateOf<EstadoLista<Team>>(EstadoLista.Cargando) }
    var recarga by remember { mutableIntStateOf(0) }
    var aEliminar by remember { mutableStateOf<Team?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(recarga) {
        val profile = container.authRepository.getCurrentUser().first { it !is Result.Loading }
        val user = (profile as? Result.Success)?.data
        if (profile is Result.Error) estado = EstadoLista.Error(profile.message)
        else if (user == null) estado = EstadoLista.Error("Inicia sesión para ver equipos")
        else if (RolePolicy.isStaff(user.role)) container.teamRepository.getAllTeams().collect { estado = it.aEstadoLista() }
        else if (user.teamId.isBlank()) estado = EstadoLista.Datos(emptyList())
        else container.teamRepository.getTeamById(user.teamId).collect { value ->
            estado = when (value) {
                is Result.Success -> EstadoLista.Datos(listOf(value.data))
                is Result.Error -> EstadoLista.Error(value.message)
                is Result.Loading -> EstadoLista.Cargando
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (pickTrainings || pickMatches) "Elegí un equipo" else "Equipos") },
                navigationIcon = {
                    IconButton(onClick = { navController.safeBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            if (esStaff && !(pickTrainings || pickMatches)) {
                FloatingActionButton(onClick = { navController.navigate("createTeam") }) {
                    Icon(Icons.Filled.Add, contentDescription = "Crear equipo")
                }
            }
        }
    ) { padding ->
        ContenidoLista(
            estado = estado,
            textoVacio = "Todavía no hay equipos registrados.",
            modifier = Modifier.padding(padding)
        ) { equipos ->
            LazyColumn(
                modifier = Modifier.padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                // Agrupados por categoría (sub-10, sub-15, primera…)
                items(equipos.sortedBy { it.category.ordinal }) { equipo ->
                    Card(modifier = Modifier.fillMaxWidth().then(
                        if (pickTrainings || pickMatches) Modifier.clickable(enabled = equipo.id.isNotBlank()) {
                            navController.navigate("${if (pickMatches) "matches" else "trainings"}/${Uri.encode(equipo.id)}")
                        } else Modifier
                    )) {
                        Column(Modifier.padding(16.dp)) {
                            Row {
                                Column(Modifier.weight(1f)) {
                                    Text(equipo.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Categoría: ${equipo.category.name.replace('_', '-')}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (esStaff && !(pickTrainings || pickMatches)) {
                                    TextButton(onClick = { navController.navigate("editTeam/${Uri.encode(equipo.id)}") }) { Text("Editar") }
                                    IconButton(onClick = { aEliminar = equipo }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar equipo")
                                    }
                                }
                            }
                            if (!(pickTrainings || pickMatches)) {
                                Spacer(Modifier.width(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(enabled = equipo.id.isNotBlank(), onClick = {
                                        if (equipo.id.isNotBlank()) navController.navigate("players/${Uri.encode(equipo.id)}")
                                    }) {
                                        Text("Jugadores")
                                    }
                                    OutlinedButton(enabled = equipo.id.isNotBlank(), onClick = {
                                        if (equipo.id.isNotBlank()) navController.navigate("trainings/${Uri.encode(equipo.id)}")
                                    }) {
                                        Text("Entrenamientos")
                                    }
                                }
                                }
                        }
                    }
                }
            }
        }
    }

    aEliminar?.let { equipo ->
        AlertDialog(
            onDismissRequest = { aEliminar = null },
            title = { Text("Eliminar equipo") },
            text = { Text("¿Eliminar \"${equipo.name}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        container.teamRepository.deleteTeam(equipo.id).collect { }
                        aEliminar = null
                        recarga++
                    }
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { aEliminar = null }) { Text("Cancelar") } }
        )
    }
}
