package pe.edu.esan.sportpro.ui.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import pe.edu.esan.sportpro.data.model.UserRole

class ProfileRoleControlsTest {
    @get:Rule val rule = createComposeRule()
    @Test fun playerSeesActualRoleWithoutAnyPreselectedRequest() {
        rule.setContent {
            MaterialTheme { Column {
                var target by remember { mutableStateOf(ProfileRoleUi.initialTarget) }
                ProfileRoleSummary(UserRole.JUGADOR, false, false, false)
                RoleRequestControls(UserRole.JUGADOR, target, false, false, false, false, { target = it }, {})
            } }
        }
        rule.onNodeWithText("Rol: Jugador").assertExists()
        rule.onNodeWithText("Solicitar cambio de rol").assertExists()
        listOf(UserRole.ADMIN, UserRole.ENTRENADOR, UserRole.PADRE).forEach {
            rule.onNodeWithTag("role_request_${it.name}").assertIsNotSelected()
        }
        rule.onNodeWithTag("role_request_submit").assertIsNotEnabled()
        rule.onNodeWithTag("role_request_PADRE").performClick()
        rule.onNodeWithTag("role_request_submit").assertIsEnabled()
        rule.onNodeWithText("Rol: Jugador").assertExists()
    }
    @Test fun loadingStateDoesNotShowSignedOutOrEnableRequest() {
        rule.setContent { MaterialTheme { Column {
            ProfileRoleSummary(null, true, false, false)
            RoleRequestControls(null, null, true, false, false, false, {}, {})
        } } }
        rule.onNodeWithText("Rol: Cargando…").assertExists()
        rule.onNodeWithText("Rol: Sin sesión").assertDoesNotExist()
        rule.onNodeWithTag("role_request_submit").assertIsNotEnabled()
    }
}
