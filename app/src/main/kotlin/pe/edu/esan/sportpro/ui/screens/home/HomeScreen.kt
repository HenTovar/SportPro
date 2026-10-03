package pe.edu.esan.sportpro.ui.screens.home

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import pe.edu.esan.sportpro.data.model.*
import pe.edu.esan.sportpro.data.repository.Result
import pe.edu.esan.sportpro.di.DefaultAppContainer

@Composable
fun HomeScreen(navController: NavHostController) {
    val container = remember { DefaultAppContainer() }
    var user by remember { mutableStateOf<User?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        container.authRepository.getCurrentUser().collect { result ->
            when (result) {
                is Result.Success -> { user = result.data; loading = false }
                is Result.Error -> { error = result.message; loading = false }
                is Result.Loading -> loading = true
            }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("SportPro", style = MaterialTheme.typography.headlineLarge)
        Text("Bienvenido, ${user?.name.orEmpty()}")
        Text("Rol: ${RolePolicy.label(user?.role)}", style = MaterialTheme.typography.titleMedium)
        if (loading) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        val profile = user
        if (profile != null) {
            val staff = RolePolicy.isStaff(profile.role)
            val assigned = profile.teamId.isNotBlank() && profile.playerId.isNotBlank()
            if (!staff) androidx.compose.foundation.text.selection.SelectionContainer {
                Text("Código de cuenta para tu entrenador: ${profile.uid}", style = MaterialTheme.typography.bodySmall)
            }
            if (!staff && !assigned) Text("Tu entrenador debe vincular tu cuenta con tu ficha o la de tu hijo. Todavía no tienes un equipo asignado.")
            if (staff || assigned) {
                val team = Uri.encode(profile.teamId)
                if (!staff) Button(onClick = { navController.navigate("players/$team") }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (profile.role == UserRole.PADRE) "Información de mi hijo" else "Mi información")
                }
                Button(onClick = { navController.navigate("teams") }, modifier = Modifier.fillMaxWidth()) { Text(if (staff) "Equipos" else "Mi equipo") }
                Button(onClick = { navController.navigate(if (staff) "teams?pick=trainings" else "trainings/$team") }, modifier = Modifier.fillMaxWidth()) { Text("Entrenamientos y horarios") }
                Button(onClick = { navController.navigate(if (staff) "teams?pick=matches" else "matches/$team") }, modifier = Modifier.fillMaxWidth()) { Text("Partidos") }
                if (!staff) OutlinedButton(onClick = { navController.navigate("attendanceHistory/$team/${Uri.encode(profile.playerId)}") }, modifier = Modifier.fillMaxWidth()) { Text("Calendario de asistencia") }
            }
        }
        OutlinedButton(onClick = {
            container.authRepository.logout()
            navController.navigate("login") { popUpTo("home") { inclusive = true } }
        }) { Text("Cerrar sesión") }
    }
}
