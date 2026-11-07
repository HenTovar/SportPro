package pe.edu.esan.sportpro.ui.guided

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

/** Repository authorizes the actor; these controls never choose another user's UID. */
@Composable
fun TeamMembershipControls(teamId: String?, teamName: String?, busy: Boolean,
    onJoin: (String) -> Unit, onLeave: (String) -> Unit) {
    var code by remember(teamId) { mutableStateOf("") }
    var confirming by remember(teamId) { mutableStateOf(false) }
    Text("Mi equipo", style = MaterialTheme.typography.titleMedium)
    if (teamId.isNullOrBlank()) {
        Text("Pide el código de equipo a su entrenador. Al unirte se creará tu ficha con los datos de tu perfil.")
        OutlinedTextField(code, { code = it.trim().take(64) }, label = { Text("Código de equipo") },
            enabled = !busy, singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth().testTag("team_join_code"))
        Button(enabled = !busy && code.isNotBlank(), onClick = { onJoin(code.trim()) },
            modifier = Modifier.heightIn(min = 48.dp).testTag("team_join_submit")) { Text("Unirme a un equipo") }
    } else {
        Text(teamName?.takeIf { it.isNotBlank() } ?: "Equipo actual")
        Text("Puedes retirarte por decisión propia. Tu ficha anterior quedará inactiva y se conservará el historial.")
        OutlinedButton(enabled = !busy, onClick = { confirming = true },
            modifier = Modifier.heightIn(min = 48.dp).testTag("team_leave")) { Text("Retirarme del equipo") }
        if (confirming) AlertDialog(onDismissRequest = { if (!busy) confirming = false },
            title = { Text("¿Retirarte del equipo?") },
            text = { Text("Después podrás unirte a otro con un código nuevo. Tu historial no se borra.") },
            confirmButton = { TextButton(enabled = !busy, onClick = { confirming = false; onLeave(teamId) },
                modifier = Modifier.testTag("team_leave_confirm")) { Text("Sí, retirarme") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { confirming = false }) { Text("Cancelar") } })
    }
}
