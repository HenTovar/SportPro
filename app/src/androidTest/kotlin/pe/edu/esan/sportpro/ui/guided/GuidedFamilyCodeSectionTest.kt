package pe.edu.esan.sportpro.ui.guided

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import pe.edu.esan.sportpro.data.model.User
import pe.edu.esan.sportpro.data.model.UserRole

class GuidedFamilyCodeSectionTest {
    @get:Rule val rule = createComposeRule()
    private val admin = User(uid = "admin", role = UserRole.ADMIN, isActive = true)
    @Test fun loadedAdminEditorPassesExactTeamAndPlayerToIssuer() {
        var target: Pair<String, String>? = null
        rule.setContent { MaterialTheme {
            GuidedFamilyCodeSection(admin, "team-a", "player-a", true, false) { team, player ->
                target = team to player
                Text("Generar código para padre")
            }
        } }
        rule.onNodeWithText("Generar código para padre").assertExists()
        rule.onNodeWithText("Vincular padre o apoderado").assertExists()
        rule.runOnIdle { assertEquals("team-a" to "player-a", target) }
    }
    @Test fun coachDoesNotInstantiateIssuer() {
        rule.setContent { MaterialTheme {
            GuidedFamilyCodeSection(admin.copy(role = UserRole.ENTRENADOR, coachTeamIds = listOf("team-a")), "team-a", "player-a", true, false) { _, _ -> error("issuer must not be composed") }
        } }
        rule.onNodeWithText("Vincular padre o apoderado").assertDoesNotExist()
    }
    @Test fun unloadedPlayerDoesNotInstantiateIssuer() {
        rule.setContent { MaterialTheme {
            GuidedFamilyCodeSection(admin, "team-a", "player-a", false, false) { _, _ -> error("issuer must not be composed") }
        } }
        rule.onNodeWithText("Vincular padre o apoderado").assertDoesNotExist()
    }
    @Test fun busyEditorDoesNotInstantiateIssuer() {
        rule.setContent { MaterialTheme {
            GuidedFamilyCodeSection(admin, "team-a", "player-a", true, true) { _, _ -> error("issuer must not be composed") }
        } }
        rule.onNodeWithText("Vincular padre o apoderado").assertDoesNotExist()
    }
}
