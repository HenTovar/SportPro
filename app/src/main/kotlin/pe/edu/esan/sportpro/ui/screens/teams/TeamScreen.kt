package pe.edu.esan.sportpro.ui.screens.teams

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import pe.edu.esan.sportpro.ui.common.BackButton
import pe.edu.esan.sportpro.ui.navigation.*
import pe.edu.esan.sportpro.ui.screens.players.PlayersScreen
import pe.edu.esan.sportpro.ui.screens.trainings.TrainingsScreen
import pe.edu.esan.sportpro.ui.screens.home.MatchesScreen
import pe.edu.esan.sportpro.ui.screens.community.CommunityScreen

/** One logical team container; child screens keep their existing access checks. */
@Composable
fun TeamScreen(teamId: String, initialTab: String, nav: NavHostController) {
    val session = LocalTeamSession.current
    val contentState = rememberSaveableStateHolder()
    var tab by rememberSaveable(teamId) { mutableStateOf(initialTab.takeIf { it in StructurePolicy.teamTabs } ?: "players") }
    BindSelectedTeam(teamId)
    Scaffold(topBar = {
        Column {
            TopAppBar(title = { Text(session.teams.find { it.id == teamId }?.name ?: "Ficha del equipo") },
                navigationIcon = { pe.edu.esan.sportpro.ui.navigation.PrimaryDestinationBackButton(nav) },
                actions = { TextButton(onClick = { nav.navigate("teams") }) { Text("Cambiar") } })
            ScrollableTabRow(selectedTabIndex = StructurePolicy.teamTabs.indexOf(tab), edgePadding = 0.dp) {
                listOf("Plantel", "Entrenamientos", "Partidos", "Muro").forEachIndexed { index, title ->
                    Tab(selected = tab == StructurePolicy.teamTabs[index], onClick = { tab = StructurePolicy.teamTabs[index] }, text = { Text(title, maxLines = 1) })
                }
            }
        }
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            contentState.SaveableStateProvider("$teamId/$tab") {
                when (tab) {
                    "players" -> PlayersScreen(teamId, nav, embedded = true)
                    "trainings" -> TrainingsScreen(teamId, nav, embedded = true)
                    "matches" -> MatchesScreen(teamId, nav, embedded = true)
                    else -> CommunityScreen(teamId, nav, embedded = true)
                }
            }
        }
    }
}
