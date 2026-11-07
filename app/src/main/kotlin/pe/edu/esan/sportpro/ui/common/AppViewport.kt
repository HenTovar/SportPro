package pe.edu.esan.sportpro.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/** Paint the whole window first. System bars/cutout only; IME belongs to the content frame, not the bottom bar. */
@Composable
fun AppViewport(insets: WindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout), content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxSize().testTag("app_window"), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(insets)) { content() }
    }
}
