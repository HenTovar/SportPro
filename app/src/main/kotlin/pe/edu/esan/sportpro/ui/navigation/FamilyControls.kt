package pe.edu.esan.sportpro.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.FamilyPolicy
import pe.edu.esan.sportpro.data.repository.FamilyRepository

@Composable
fun FamilyLinkControls(session: TeamSession) {
    val repo = remember { FamilyRepository(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance()) }
    val scope = rememberCoroutineScope()
    var code by rememberSaveable(session.account?.uid) { mutableStateOf("") }
    var message by remember(session.account?.uid) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val children = FamilyPolicy.children(session.account)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Mis hijos", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { expanded = true }, enabled = children.isNotEmpty() && !busy) {
                Text(session.child?.let { "${it.name} · ${session.teams.find { t -> t.id == it.teamId }?.name ?: "Equipo"}" } ?: "Aún no hay hijos vinculados")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                children.forEach { child -> DropdownMenuItem(text = { Text("${child.name} · ${session.teams.find { it.id == child.teamId }?.name ?: "Equipo"}") },
                    onClick = { session.selectChild(child.key); expanded = false }) }
            }
            Text("Pide al entrenador un código de vínculo. Sirve una sola vez durante 20 minutos; no necesitas copiar UIDs.")
            OutlinedTextField(value = code, onValueChange = { code = it.take(24) }, label = { Text("Código de vínculo") }, singleLine = true,
                enabled = !busy, modifier = Modifier.fillMaxWidth())
            Button(enabled = !busy && FamilyPolicy.validCode(FamilyPolicy.normalizeCode(code)), onClick = {
                busy = true; message = ""
                scope.launch {
                    try { repo.redeem(code); code = ""; message = "Hijo vinculado. Actualizando tu lista…"; session.refresh() }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { message = "No se pudo vincular: código inválido, vencido, usado o no disponible para esta familia. Pide uno nuevo al entrenador." }
                    finally { busy = false }
                }
            }) { Text(if (busy) "Vinculando…" else "Vincular otro hijo") }
            if (message.isNotBlank()) Text(message)
        }
    }
}

@Composable
fun FamilyCodeIssuer(teamId: String, playerId: String?) {
    val repo = remember { FamilyRepository(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance()) }
    val scope = rememberCoroutineScope()
    var code by remember(teamId, playerId) { mutableStateOf("") }
    var error by remember(teamId, playerId) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    Text("Vínculo familiar", style = MaterialTheme.typography.titleSmall)
    Text("Guarda la ficha y genera un código para que el padre lo introduzca en Inicio. Cada padre puede vincular varios hijos.")
    OutlinedButton(enabled = playerId != null && !busy, onClick = {
        busy = true; error = ""; code = ""
        scope.launch {
            try { code = repo.issue(teamId, playerId!!) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = "No se pudo emitir el código. Comprueba la conexión y que las nuevas reglas familiares estén publicadas." }
            finally { busy = false }
        }
    }) { Text(if (busy) "Generando…" else "Generar código para padre") }
    if (code.isNotBlank()) SelectionContainer { Text("Código: ${code.chunked(4).joinToString("-")} · vence en 20 min · un solo uso") }
    if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
    FamilyRevocationControls(teamId, playerId)
}
