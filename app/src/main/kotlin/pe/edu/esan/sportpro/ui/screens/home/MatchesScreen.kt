package pe.edu.esan.sportpro.ui.screens.home

import pe.edu.esan.sportpro.ui.navigation.safeBack

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.RolePolicy
import pe.edu.esan.sportpro.data.repository.Result
import pe.edu.esan.sportpro.di.DefaultAppContainer
import java.text.SimpleDateFormat
import java.util.*

private data class MatchRow(val id: String, val name: String, val date: Long, val status: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(teamId: String, navController: NavHostController) {
    val container = remember { DefaultAppContainer() }
    var rows by remember { mutableStateOf<List<MatchRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(teamId) {
        try {
            val profile = container.authRepository.getCurrentUser().first { it !is Result.Loading }
            val user = (profile as? Result.Success)?.data
            require(RolePolicy.canReadTeam(user, teamId)) { "No tienes acceso a este equipo" }
            val collection = FirebaseFirestore.getInstance().collection("matches")
            // Cada consulta se limita al equipo autorizado; no descarga partidos ajenos.
            val docs = collection.whereEqualTo("teamA", teamId).get().await().documents +
                collection.whereEqualTo("teamB", teamId).get().await().documents
            rows = docs.distinctBy { it.id }.filter { it.getBoolean("active") ?: it.getBoolean("isActive") ?: true }.map {
                val raw = it.get("date")
                val date = when (raw) { is com.google.firebase.Timestamp -> raw.toDate().time; is Number -> raw.toLong(); else -> 0L }
                MatchRow(it.id, it.getString("name") ?: "Partido", date, it.getString("status").orEmpty())
            }.sortedBy { it.date }
        } catch (e: Exception) { error = e.message ?: "No se pudieron cargar los partidos" }
        finally { loading = false }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Partidos del equipo") }, navigationIcon = {
        TextButton(onClick = { navController.safeBack() }) { Text("Volver") }
    }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            if (loading) CircularProgressIndicator()
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (!loading && error == null && rows.isEmpty()) Text("No hay partidos programados para este equipo.")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(rows, key = { it.id }) { row ->
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                        Text(row.name, style = MaterialTheme.typography.titleMedium)
                        Text(if (row.date > 0) SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(row.date)) else "Fecha pendiente")
                        Text(when (row.status) { "not_started" -> "Programado"; "in_progress" -> "En curso"; "finished" -> "Finalizado"; else -> row.status })
                    } }
                }
            }
        }
    }
}
