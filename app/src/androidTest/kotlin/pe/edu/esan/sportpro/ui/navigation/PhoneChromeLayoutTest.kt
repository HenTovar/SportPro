package pe.edu.esan.sportpro.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import pe.edu.esan.sportpro.data.model.*
import pe.edu.esan.sportpro.ui.common.AppViewport
import pe.edu.esan.sportpro.ui.screens.home.MatchListContent
import pe.edu.esan.sportpro.ui.screens.home.MatchStatusFilters
import pe.edu.esan.sportpro.ui.theme.SportProTheme

/** Real Compose layout fixtures, without Auth/Firestore; must RUN on Android, compile is not acceptance. */
class PhoneChromeLayoutTest {
    @get:Rule val rule = createComposeRule()
    private fun render(density: Float = 1f, content: @Composable () -> Unit) {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density, 1f)) {
                SportProTheme(darkTheme = true) { Box(Modifier.requiredSize(411.dp, 820.dp)) { content() } }
            }
        }
    }
    private fun verifyFilters(density: Float) {
        render(density) { MatchStatusFilters("", {}) }
        rule.onNodeWithTag("match_filters").assertWidthIsEqualTo(411.dp)
        rule.onNodeWithTag("match_filter_").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithText("Finalizados").performScrollTo().assertIsDisplayed()
        val viewport=rule.onNodeWithTag("match_filters").fetchSemanticsNode().boundsInRoot
        val last=rule.onNodeWithText("Finalizados").fetchSemanticsNode().boundsInRoot
        assertTrue(last.left >= viewport.left && last.right <= viewport.right)
        assertEquals(411f*density, viewport.width, 1.1f)
    }
    @Test fun filtersAt411Dp() { verifyFilters(1f) }
    @Test fun filtersAt1080PxWith411DpViewport() { verifyFilters(1080f/411f) }
    @Test fun emptyMatchScreenDoesNotCrushFilters() {
        render { MatchListContent(emptyList(),false,null,true,false,"","",{},{},{},{}) }
        rule.onNodeWithTag("match_list").performScrollToNode(hasTestTag("match_filters"))
        rule.onNodeWithTag("match_filter_").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithText("Finalizados").performScrollTo().assertIsDisplayed()
    }
    @Test fun fixedBarKeepsScrolledFiltersAboveIt() {
        render {
            Scaffold(contentWindowInsets=WindowInsets(0,0,0,0), bottomBar={ Box(Modifier.fillMaxWidth().height(80.dp).testTag("bottom_bar")) }) { padding ->
                MatchListContent(emptyList(),false,null,true,false,"","",{},{},{},{}, Modifier.padding(padding))
            }
        }
        rule.onNodeWithTag("match_list").performScrollToNode(hasTestTag("match_filters"))
        val filter=rule.onNodeWithTag("match_filters").fetchSemanticsNode().boundsInRoot
        val bar=rule.onNodeWithTag("bottom_bar").fetchSemanticsNode().boundsInRoot
        assertTrue(filter.bottom <= bar.top)
    }
    @Test fun wallLabelFitsFiveTabBar() {
        render {
            val team=Team(id="fixture",name="Equipo de prueba")
            val user=User(uid="fixture",role=UserRole.ADMIN)
            CompositionLocalProvider(LocalTeamSession provides TeamSession(user,listOf(team),team,false)) {
                MainNavigationBar(rememberNavController(),"matches/{teamId}")
            }
        }
        rule.onNodeWithText("Muro").assertIsDisplayed()
        rule.onNodeWithText("Comunidad").assertDoesNotExist()
        val bounds=rule.onNodeWithText("Muro").fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.width <= 411f/5f)
    }
    @Test fun rootHasNoBackArrow() { render { DestinationBackButton("matches/{teamId}") {} };rule.onNodeWithContentDescription("Volver").assertDoesNotExist() }
    @Test fun detailStillHasBackArrow() { render { DestinationBackButton("liveMatch/{matchId}") {} };rule.onNodeWithContentDescription("Volver").assertIsDisplayed() }
    @Test fun oneConsumedTopInsetNotTwo() {
        render {
            AppViewport(WindowInsets(top=24.dp)) {
                Scaffold(topBar={TopAppBar(title={Text("Cabecera",Modifier.testTag("header"))}, windowInsets=WindowInsets(top=24.dp))}) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding).testTag("body"))
                }
            }
        }
        // Root applies 24; nested app bar must consume it instead of adding another 24.
        val body=rule.onNodeWithTag("body").fetchSemanticsNode().boundsInRoot
        val window=rule.onNodeWithTag("app_window").fetchSemanticsNode().boundsInRoot
        assertEquals(24f+64f,body.top-window.top,1.1f)
    }
}
