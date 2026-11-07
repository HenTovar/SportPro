package pe.edu.esan.sportpro.features

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test
import pe.edu.esan.sportpro.data.model.*
import pe.edu.esan.sportpro.ui.navigation.*
import pe.edu.esan.sportpro.ui.theme.*

/** Instrumentation required: compilation alone is not a layout verdict. */
class FeatureAccessibilityTest {
    @get:Rule val rule=createComposeRule()
    private fun render(dark:Boolean,font:Float) {
        val at=Month.now().calendar().timeInMillis
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f,font),
                LocalTeamSession provides TeamSession(profile=User(uid="fixture",role=UserRole.ADMIN),selected=Team(id="a",name="Equipo de prueba"),loading=false),
                LocalTeamFeatures provides FeatureState(team="a",matches=listOf(LiveMatch(id="m",name="Partido con nombre largo de prueba",date=at)),cached=false,loading=false)) {
                MaterialTheme(colorScheme=if(dark)DarkColorScheme else LightColorScheme,typography=Typography) {
                    Box(Modifier.requiredSize(411.dp,820.dp)) { TeamFeaturesScreen("a",rememberNavController()) }
                }
            }
        }
    }
    private fun calendar(dark:Boolean,font:Float) {
        render(dark,font)
        rule.onNodeWithTag("team_features").performScrollToNode(hasTestTag("monthly_calendar"))
        rule.onNodeWithTag("calendar_day_15").assertIsDisplayed().performClick()
        rule.onNodeWithTag("team_features").performScrollToNode(hasText("Día 15"))
        rule.onNodeWithText("Día 15").assertIsDisplayed()
        rule.onNodeWithTag("team_features").performScrollToNode(hasText("Estadísticas"))
        rule.onNodeWithText("Estadísticas").performClick()
        rule.onNodeWithTag("team_features").performScrollToNode(hasText("Asistencia del mes"))
        rule.onNodeWithText("Asistencia del mes").assertIsDisplayed()
    }
    @Test fun lightLarge130(){calendar(false,1.3f)}
    @Test fun lightLarge150(){calendar(false,1.5f)}
    @Test fun darkLarge130(){calendar(true,1.3f)}
    @Test fun darkLarge150(){calendar(true,1.5f)}
}
