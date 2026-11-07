package pe.edu.esan.sportpro.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import pe.edu.esan.sportpro.ui.common.BackButton

@Composable
fun MainNavigationBar(nav: NavHostController, route: String) {
    val session = LocalTeamSession.current
    val team = session.selected?.id
    val largeText=androidx.compose.ui.platform.LocalDensity.current.fontScale>1.2f
    NavigationBar(modifier=Modifier.heightIn(min=if(largeText)104.dp else 80.dp)) {
        StructurePolicy.destinations(session.profile).forEach { label ->
            val selected = when (label) {
                "Inicio" -> route == "home"
                "Perfil" -> route == "profile" || route == "adminRequests"
                "Partidos" -> route.startsWith("matches") || route.startsWith("liveMatch")
                "Comunidad" -> route.startsWith("community")
                else -> route.startsWith("team") || route.startsWith("players") || route.contains("Training") || route.startsWith("trainings") || route.startsWith("attendance")
            }
            val icon = when (label) {
                "Inicio" -> Icons.Default.Home
                "Partidos" -> Icons.Default.PlayArrow
                "Equipo" -> Icons.Default.List
                "Comunidad" -> Icons.Default.Favorite
                else -> Icons.Default.Person
            }
            NavigationBarItem(selected = selected, onClick = {
                val destination = when (label) {
                    "Inicio" -> "home"
                    "Perfil" -> "profile"
                    "Equipo" -> team?.let { StructurePolicy.team(it) } ?: "teams"
                    "Partidos" -> team?.let { "matches/${StructurePolicy.component(it)}" } ?: "teams?pick=matches"
                    else -> team?.let { "community/${StructurePolicy.component(it)}" } ?: "teams?pick=community"
                }
                nav.navigate(destination) {
                    popUpTo("home") { inclusive = false }
                    launchSingleTop = true
                }
            }, icon = { Icon(icon, contentDescription = label) }, label = { Text(StructurePolicy.navigationLabel(label), maxLines = 2, softWrap = true) })
        }
    }
}

@Composable
fun TeamSelector(modifier: Modifier = Modifier, onManage: (() -> Unit)? = null) {
    val session = LocalTeamSession.current
    var expanded by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = { expanded = true }, enabled = !session.loading && session.teams.isNotEmpty(), modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Icon(Icons.Default.Person, null); Spacer(Modifier.width(8.dp))
                Text(session.selected?.name ?: "Seleccionar equipo", maxLines = 2)
            }
            onManage?.let { TextButton(onClick = it) { Text("Ver equipos") } }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            session.teams.filter { it.isActive }.forEach { team ->
                DropdownMenuItem(text = { Text(team.name) }, onClick = { session.select(team.id); expanded = false })
            }
        }
    }
}

/** Root destinations already have the persistent bar; details keep back navigation. */
@Composable
fun DestinationBackButton(route: String, onBack: () -> Unit) {
    if (!StructurePolicy.isPrimaryDestination(route)) BackButton(onBack)
}

@Composable
fun PrimaryDestinationBackButton(nav: NavHostController) {
    val entry by nav.currentBackStackEntryAsState()
    DestinationBackButton(entry?.destination?.route.orEmpty()) { nav.safeBack() }
}
