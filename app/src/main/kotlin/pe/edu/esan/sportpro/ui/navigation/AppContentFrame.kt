package pe.edu.esan.sportpro.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag

/** Keyboard occlusion is applied only to content. Never lift navigation above the keyboard. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppContentFrame(showNavigation: Boolean,
    keyboardOpen: Boolean = WindowInsets.isImeVisible || WindowInsets.ime.getBottom(LocalDensity.current) > 0,
    keyboardInsets: WindowInsets = WindowInsets.ime,
    bottomBar: @Composable () -> Unit,
    content: @Composable () -> Unit) {
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = {
        if (StructurePolicy.bottomBarVisible(showNavigation, keyboardOpen)) {
            Box(Modifier.testTag("primary_navigation")) { bottomBar() }
        }
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
            .windowInsetsPadding(keyboardInsets).testTag("app_content")) { content() }
    }
}
