package pe.edu.esan.sportpro.ui.guided

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import pe.edu.esan.sportpro.data.model.UserRole
import pe.edu.esan.sportpro.ui.navigation.LocalTeamSession
import pe.edu.esan.sportpro.ui.navigation.safeBack

@Composable
fun MyDataScreen(nav: NavHostController, vm: GuidedAccountViewModel = viewModel {
    GuidedAccountViewModel(pe.edu.esan.sportpro.di.DefaultAppContainer().guidedAccounts)
}) {
    val state by vm.state.collectAsState()
    val session = LocalTeamSession.current
    LaunchedEffect(vm) { vm.load() }
    LaunchedEffect(state.revision) { if (state.revision > 0) session.refresh() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Mis datos", style = MaterialTheme.typography.headlineMedium)
        if (state.busy) CircularProgressIndicator()
        state.message?.let { Text(it, Modifier.testTag("my_data_status")) }
        if (state.loaded) {
            if (state.coachPending) Text("Entrenador: Pendiente de aprobación. Tus datos están guardados.")
            GuidedDetailsForm(state.draft, FormAccess.SELF, state.busy, state.errors, vm::change)
            Button(enabled = !state.busy, onClick = vm::save,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("my_data_save")) { Text("Guardar mis datos") }
            if (!session.loading && session.error == null && session.account?.role == UserRole.JUGADOR && !state.coachPending) {
                HorizontalDivider()
                val teamId = session.account?.teamId?.takeIf { it.isNotBlank() }
                TeamMembershipControls(teamId, session.teams.find { it.id == teamId }?.name,
                    state.busy, vm::join, vm::leave)
            }
        } else if (!state.busy) OutlinedButton(onClick = vm::load) { Text("Volver a cargar") }
        OutlinedButton(enabled = !state.busy, onClick = { nav.safeBack() }) { Text("Volver") }
    }
}
