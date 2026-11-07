package pe.edu.esan.sportpro.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Paint the whole window first. One inset owner, consumed for nested scaffolds. */
@Composable
fun AppViewport(insets: WindowInsets = WindowInsets.safeDrawing, content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(insets)) { content() }
    }
}
