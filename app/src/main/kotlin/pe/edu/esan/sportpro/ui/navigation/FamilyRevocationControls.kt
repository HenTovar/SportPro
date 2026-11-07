package pe.edu.esan.sportpro.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.FamilyPolicy
import pe.edu.esan.sportpro.data.model.UserRole
import pe.edu.esan.sportpro.data.repository.FamilyRepository

private data class RevocableParent(val uid: String, val label: String)

@Composable
fun FamilyRevocationControls(teamId: String, playerId: String?) {
    val session = LocalTeamSession.current
    if (session.account?.role != UserRole.ADMIN || session.account.isActive != true || playerId == null) return
    val db = remember { FirebaseFirestore.getInstance() }
    val repo = remember { FamilyRepository(db, FirebaseAuth.getInstance()) }
    val scope = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }
    var parents by remember(teamId, playerId) { mutableStateOf<List<RevocableParent>>(emptyList()) }
    var selected by remember(teamId, playerId) { mutableStateOf("") }
    var childName by remember(teamId, playerId) { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var message by remember(teamId, playerId) { mutableStateOf("") }
    LaunchedEffect(teamId, playerId, revision) {
        loading = true
        try {
            val child = db.collection("teams").document(teamId).collection("players").document(playerId).get().await()
            childName = child.getString("name").orEmpty()
            val owner = child.getString("parentUid").orEmpty()
            val docs = db.collection("users").whereEqualTo("role", "PADRE").get().await().documents.toMutableList()
            if (owner.isNotBlank() && docs.none { it.id == owner }) {
                val old = db.collection("users").document(owner).get().await()
                if (old.exists()) docs += old
            }
            parents = docs.filter { account ->
                account.id == owner || (account.getString("teamId") == teamId && account.getString("playerId") == playerId) ||
                    FamilyPolicy.decode(account.get("children")).any { it.teamId == teamId && it.playerId == playerId }
            }.map { RevocableParent(it.id, it.getString("name").orEmpty() + " · " + it.getString("email").orEmpty()) }.distinctBy { it.uid }
            selected = selected.takeIf { id -> parents.any { it.uid == id } } ?: parents.firstOrNull()?.uid.orEmpty()
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { message = "No se pudieron cargar los vínculos. Actualiza antes de revocar."; parents = emptyList() }
        finally { loading = false }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Administrar vínculo familiar", style = MaterialTheme.typography.titleSmall)
            Text("Retira sólo el acceso de esta cuenta a esta ficha y asistencia; no borra al jugador ni a sus hermanos.")
            OutlinedButton(onClick = { expanded = true }, enabled = !loading && !busy && parents.isNotEmpty()) {
                Text(parents.find { it.uid == selected }?.label ?: if (loading) "Cargando vínculos…" else "Sin vínculos para revocar")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                parents.forEach { parent -> DropdownMenuItem(text = { Text(parent.label) }, onClick = { selected = parent.uid; expanded = false }) }
            }
            OutlinedButton(enabled = !loading && !busy && selected.isNotBlank(), onClick = { confirm = true }) { Text("Revocar 1 vínculo") }
            TextButton(enabled = !busy, onClick = { revision++ }) { Text("Actualizar vínculos") }
            if (message.isNotBlank()) Text(message)
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { if (!busy) confirm = false },
        title = { Text("Revocar 1 vínculo familiar") },
        text = { Text(parents.find { it.uid == selected }?.label.orEmpty() + " perderá acceso a " + childName + ". Sus otros hijos se conservan. Se necesitará un código nuevo para volver a vincular.") },
        confirmButton = { TextButton(enabled = !busy, onClick = {
            confirm = false; busy = true; message = ""
            scope.launch {
                try { repo.revoke(selected, teamId, playerId); message = "Vínculo revocado. El servidor ya no autoriza esta ficha para esa cuenta."; revision++ }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { message = "No se pudo revocar; no se confirmó ningún cambio. Recarga y comprueba que las nuevas reglas estén publicadas." }
                finally { busy = false }
            }
        }) { Text("Confirmar revocación") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar") } })
}
