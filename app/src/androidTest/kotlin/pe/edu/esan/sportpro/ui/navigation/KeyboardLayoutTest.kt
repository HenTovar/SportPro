package pe.edu.esan.sportpro.ui.navigation

import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import pe.edu.esan.sportpro.ui.common.AppViewport
import pe.edu.esan.sportpro.ui.screens.home.MatchListContent
import pe.edu.esan.sportpro.ui.theme.SportProTheme

/** Injected insets test layout only; the last test requires an actual software IME. */
@OptIn(ExperimentalLayoutApi::class)
class KeyboardLayoutTest {
    @get:Rule val rule = createComposeRule()
    @Volatile private var imeObserved = false
    @Composable private fun Matches() {
        var query by remember { mutableStateOf("") }
        Scaffold(topBar={TopAppBar(title={Text("Partidos del equipo")})}) { padding ->
            MatchListContent(emptyList(),false,null,true,false,query,"",{query=it},{},{},{},Modifier.padding(padding).consumeWindowInsets(padding))
        }
    }
    private fun fixture(open: State<Boolean>, density: Float = 1f) {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density)) {
                SportProTheme(darkTheme=true) {
                    Box(Modifier.requiredSize(411.dp,820.dp)) {
                        AppViewport(WindowInsets(0,0,0,0)) {
                            AppContentFrame(true, keyboardOpen=open.value,
                                keyboardInsets=WindowInsets(bottom=if(open.value)300.dp else 0.dp),
                                bottomBar={Box(Modifier.fillMaxWidth().height(80.dp))}) { Matches() }
                        }
                    }
                }
            }
        }
    }
    @Test fun openingHidesBarAndClosingRestoresIt() {
        val open=mutableStateOf(false); fixture(open)
        rule.onNodeWithTag("primary_navigation").assertExists()
        rule.runOnIdle {open.value=true}
        rule.onNodeWithTag("primary_navigation").assertDoesNotExist()
        rule.runOnIdle {open.value=false}
        rule.onNodeWithTag("primary_navigation").assertExists()
    }
    @Test fun occludedFixtureKeepsSearchAboveKeyboardAt411Dp() { verifyOcclusion(1f) }
    @Test fun occludedFixtureKeepsSearchAboveKeyboardAt1080Px() { verifyOcclusion(1080f/411f) }
    private fun verifyOcclusion(density: Float) {
        fixture(mutableStateOf(true),density)
        rule.onNodeWithTag("primary_navigation").assertDoesNotExist()
        rule.onNodeWithTag("match_list").performScrollToNode(hasTestTag("match_search"))
        rule.onNodeWithTag("match_search").assertIsDisplayed()
        val search=rule.onNodeWithTag("match_search").fetchSemanticsNode().boundsInRoot
        val content=rule.onNodeWithTag("app_content").fetchSemanticsNode().boundsInRoot
        assertTrue(search.bottom <= content.bottom)
        val create=rule.onAllNodesWithTag("create_match").fetchSemanticsNodes()
        if(create.isNotEmpty()) assertTrue(create.single().boundsInRoot.bottom <= search.top)
    }
    @Test fun actualSoftwareKeyboardKeepsTypedTextVisibleAndRestoresBar() {
        rule.setContent {
            val context=LocalContext.current
            LaunchedEffect(Unit) {
                var owner=context
                while(owner is ContextWrapper && owner !is ComponentActivity) owner=owner.baseContext
                val activity=checkNotNull(owner as? ComponentActivity) { "Android activity required" }
                activity.enableEdgeToEdge()
                activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
            val visible=WindowInsets.isImeVisible || WindowInsets.ime.getBottom(LocalDensity.current)>0
            SideEffect {imeObserved=visible}
            SportProTheme(darkTheme=true) {
                AppViewport {
                    AppContentFrame(true,bottomBar={Box(Modifier.fillMaxWidth().height(80.dp))}) { Matches() }
                }
            }
        }
        rule.onNodeWithTag("match_list").performScrollToNode(hasTestTag("match_search"))
        rule.onNodeWithTag("match_search").performClick().performTextInput("Final de prueba")
        // No forced success if the device has only a hardware keyboard or no software IME.
        rule.waitUntil(10_000) {imeObserved}
        rule.waitForIdle()
        rule.onNodeWithTag("primary_navigation").assertDoesNotExist()
        rule.onNodeWithTag("match_search").assertIsDisplayed().assertTextContains("Final de prueba")
        val search=rule.onNodeWithTag("match_search").fetchSemanticsNode().boundsInRoot
        val content=rule.onNodeWithTag("app_content").fetchSemanticsNode().boundsInRoot
        assertTrue(search.bottom <= content.bottom)
        rule.onNodeWithTag("match_search").performImeAction()
        rule.waitUntil(10_000) {!imeObserved}
        rule.onNodeWithTag("primary_navigation").assertExists()
        rule.onNodeWithTag("match_search").assertTextContains("Final de prueba")
    }
}
