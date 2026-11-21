@file:Suppress("EXPERIMENTAL_MATERIAL3_API")

package pe.edu.esan.sportpro.ui.screens.teams

import pe.edu.esan.sportpro.ui.navigation.safeBack
import pe.edu.esan.sportpro.ui.navigation.LocalTeamSession
import pe.edu.esan.sportpro.ui.navigation.StructurePolicy

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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
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
fun TeamsScreen(navController: NavHostController, pickTrainings: Boolean = false, pickMatches: Boolean = false, pickCommunity: Boolean = false) {
    val container = remember { DefaultAppContainer() }
    val session = LocalTeamSession.current
    val esStaff = rememberEsStaff(container.authRepository)
    var estado by remember { mutableStateOf<EstadoLista<Team>>(EstadoLista.Cargando) }
    var recarga by remember { mutableIntStateOf(0) }
    var aEliminar by remember { mutableStateOf<Team?>(null) }
    var deactivating by remember { mutableStateOf(false) }
    var deactivateError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(recarga) {
        val profile = container.authRepository.getCurrentUser().first { it !is Result.Loading }
        val user = (profile as? Result.Success)?.data
        if (profile is Result.Error) estado = EstadoLista.Error(profile.message)
        else if (user == null) estado = EstadoLista.Error("Inicia sesión para ver equipos")
        else if (RolePolicy.isStaff(user.role)) container.teamRepository.getAllTeams().collect { estado = it.aEstadoLista() }
        else {
            val ids = (pe.edu.esan.sportpro.data.model.FamilyPolicy.children(user).map { it.teamId } + user.teamId).filter { it.isNotBlank() }.distinct()
            val rows = mutableListOf<Team>()
            for (id in ids) {
                when (val result = container.teamRepository.getTeamById(id).first { it !is Result.Loading }) {
                    is Result.Success -> rows += result.data
                    is Result.Error -> { estado = EstadoLista.Error(result.message); return@LaunchedEffect }
                    else -> Unit
                }
            }
            estado = EstadoLista.Datos(rows)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (pickTrainings || pickMatches || pickCommunity) "Elige un equipo" else "Equipos") },
                navigationIcon = {
                    pe.edu.esan.sportpro.ui.navigation.PrimaryDestinationBackButton(navController)
                }
            )
        },
        floatingActionButton = {
            if (esStaff && !(pickTrainings || pickMatches || pickCommunity)) {
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
                        if (pickTrainings || pickMatches || pickCommunity) Modifier.clickable(enabled = equipo.id.isNotBlank()) {
                            session.select(equipo.id)
                            val destination = when {
                                pickCommunity -> "community/${Uri.encode(equipo.id)}"
                                pickMatches -> "matches/${Uri.encode(equipo.id)}"
                                else -> StructurePolicy.team(equipo.id, "trainings")
                            }
                            navController.navigate(destination)
                        } else Modifier.clickable(enabled = equipo.id.isNotBlank()) {
                            session.select(equipo.id)
                            navController.navigate(StructurePolicy.team(equipo.id))
                        }
                    )) {
                        Column(Modifier.padding(16.dp)) {
                            Row {
                                Column(Modifier.weight(1f)) {
                                    Text(equipo.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Categoría: ${pe.edu.esan.sportpro.features.UiInteractionPolicy.categoryLabel(equipo.category)}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (esStaff && !(pickTrainings || pickMatches || pickCommunity)) {
                                    TextButton(onClick = { navController.navigate("editTeam/${Uri.encode(equipo.id)}") }) { Text("Editar") }
                                    IconButton(onClick = { if (!deactivating) { deactivateError = null; aEliminar = equipo } }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Desactivar equipo")
                                    }
                                }
                            }
                            if (!(pickTrainings || pickMatches || pickCommunity)) {
                                Spacer(Modifier.width(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(enabled = equipo.id.isNotBlank(), onClick = {
                                        if (equipo.id.isNotBlank()) session.select(equipo.id); navController.navigate(StructurePolicy.team(equipo.id, "players"))
                                    }) {
                                        Text("Jugadores")
                                    }
                                    OutlinedButton(enabled = equipo.id.isNotBlank(), onClick = {
                                        if (equipo.id.isNotBlank()) session.select(equipo.id); navController.navigate(StructurePolicy.team(equipo.id, "trainings"))
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
            onDismissRequest = { if (!deactivating) aEliminar = null },
            title = { Text("Desactivar equipo") },
            text = { Column {
                Text("¿Desactivar ${equipo.name}? Se oculta de los equipos activos; no se borran fichas ni historial.")
                deactivateError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = {
                TextButton(enabled = !deactivating, onClick = {
                    if (!deactivating) {
                        deactivating = true; deactivateError = null
                        scope.launch {
                            try {
                                val result = withTimeout(15_000) { container.teamRepository.deleteTeam(equipo.id).first { it !is Result.Loading } }
                                when (result) {
                                    is Result.Success -> {
                                        if (result.data) { aEliminar = null; recarga++; session.refresh() }
                                        else deactivateError = "No se confirmó la desactivación. Revisa el estado del equipo."
                                    }
                                    is Result.Error -> deactivateError = result.message
                                    else -> deactivateError = "No se confirmó la desactivación."
                                }
                            } catch (_: TimeoutCancellationException) {
                                deactivateError = "No se confirmó en 15 segundos. Puede sincronizarse después; comprueba el estado antes de reintentar."
                            } catch (e: CancellationException) { throw e }
                            catch (_: Exception) { deactivateError = "No se confirmó la desactivación. Comprueba la conexión y el estado actual." }
                            finally { deactivating = false }
                        }
                    }
                }) { Text(if (deactivating) "Desactivando…" else "Desactivar equipo") }
            },
            dismissButton = { TextButton(enabled = !deactivating, onClick = { aEliminar = null }) { Text("Cancelar") } }
        )
    }
}
