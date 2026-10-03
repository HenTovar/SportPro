@file:Suppress("EXPERIMENTAL_MATERIAL3_API")

package pe.edu.esan.sportpro.ui.screens.players

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import pe.edu.esan.sportpro.data.model.Player
import pe.edu.esan.sportpro.di.DefaultAppContainer
import pe.edu.esan.sportpro.ui.common.ContenidoLista
import pe.edu.esan.sportpro.ui.common.EstadoLista
import pe.edu.esan.sportpro.ui.common.aEstadoLista
import pe.edu.esan.sportpro.ui.common.rememberEsStaff

/**
 * Jugadores de un equipo. El staff (ADMIN/ENTRENADOR) ve la ficha completa y puede
 * registrar jugadores. JUGADOR y PADRE ven una vista reducida: sin datos físicos ni
 * contactos de otros jugadores (privacidad de menores).
 */
@Composable
fun PlayersScreen(teamId: String, navController: NavHostController) {
    val container = remember { DefaultAppContainer() }
    val esStaff = rememberEsStaff(container.authRepository)
    var estado by remember { mutableStateOf<EstadoLista<Player>>(EstadoLista.Cargando) }

    LaunchedEffect(teamId) {
        container.playerRepository.getPlayersByTeam(teamId).collect { estado = it.aEstadoLista() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jugadores") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            if (esStaff) {
                FloatingActionButton(onClick = { navController.navigate("createPlayer/$teamId") }) {
                    Icon(Icons.Filled.Add, contentDescription = "Registrar jugador")
                }
            }
        }
    ) { padding ->
        ContenidoLista(
            estado = estado,
            textoVacio = "Este equipo todavía no tiene jugadores.",
            modifier = Modifier.padding(padding)
        ) { jugadores ->
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(jugadores.sortedBy { it.number }) { jugador ->
                    TarjetaJugador(
                        jugador = jugador,
                        verDatosPrivados = esStaff,
                        onHistorial = { navController.navigate("attendanceHistory/$teamId/${jugador.id}") }
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaJugador(jugador: Player, verDatosPrivados: Boolean, onHistorial: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (jugador.photoUrl.isNullOrBlank()) {
                Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(56.dp))
            } else {
                AsyncImage(
                    model = jugador.photoUrl,
                    contentDescription = "Foto de ${jugador.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(CircleShape)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("#${jugador.number}  ${jugador.name}", style = MaterialTheme.typography.titleMedium)
                Text(jugador.position.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() })
                if (verDatosPrivados) {
                    // Sólo el staff ve datos físicos y contactos.
                    Text("Talla ${jugador.height} cm · Peso ${jugador.weight} kg", style = MaterialTheme.typography.bodySmall)
                    Text("Tel: ${jugador.phone}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Emergencia: ${jugador.emergencyContactName} (${jugador.emergencyContactPhone})",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedButton(onClick = onHistorial, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Historial de asistencia")
                }
            }
        }
    }
}
