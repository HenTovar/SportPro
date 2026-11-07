package pe.edu.esan.sportpro.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import pe.edu.esan.sportpro.di.DefaultAppContainer
import pe.edu.esan.sportpro.ui.guided.GuidedAccountViewModel
import pe.edu.esan.sportpro.ui.guided.GuidedRegistrationContent

@Composable
fun RegisterScreen(navController: NavHostController, vm: GuidedAccountViewModel = viewModel {
    GuidedAccountViewModel(DefaultAppContainer().guidedAccounts)
}) {
    val state by vm.state.collectAsState()
    fun enterHome() { navController.navigate("home") {
        popUpTo(navController.graph.id) { inclusive = true }; launchSingleTop = true
    } }
    LaunchedEffect(state.registered, state.coachPending) {
        if (state.registered && !state.coachPending) enterHome()
    }
    if (state.registered && state.coachPending) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Pendiente de aprobación", style = MaterialTheme.typography.headlineMedium)
            Text("Tus datos de entrenador ya están guardados. Un administrador debe aprobar la solicitud antes de que puedas trabajar como entrenador.")
            Text("No necesitas completar otro formulario.")
            Button(onClick = { enterHome() }) { Text("Entendido, ir al inicio") }
        }
    } else GuidedRegistrationContent(state.busy, state.message, vm::register,
        onLogin = { navController.navigate("login") { popUpTo("register") { inclusive = true }; launchSingleTop = true } })
}
