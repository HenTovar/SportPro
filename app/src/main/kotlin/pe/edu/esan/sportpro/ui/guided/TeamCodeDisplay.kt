package pe.edu.esan.sportpro.ui.guided

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString

/** Displays/copies only the already generated team code; never emits or renews one. */
@Composable
internal fun TeamCodeDisplay(code: String?, allowed: Boolean, busy: Boolean, onCopied: () -> Unit) {
    if (!allowed || code.isNullOrBlank()) return
    val clipboard = LocalClipboardManager.current
    Column {
        Text(code, style = MaterialTheme.typography.headlineSmall)
        OutlinedButton(modifier = Modifier.testTag("team_code_copy"), enabled = !busy, onClick = {
            clipboard.setText(AnnotatedString(code))
            onCopied()
        }) { Text("Copiar código de equipo") }
    }
}
