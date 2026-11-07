package pe.edu.esan.sportpro.features

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Dialog controls only; the Firebase cache and back-stack need the device E2E as well. */
class AccountControlsTest {
    @get:Rule val rule = createComposeRule()
    private fun render(exit: () -> Unit) {
        rule.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalAccountActions provides AccountActions(exit)) {
                    Column { AccountControls() }
                }
            }
        }
    }
    @Test fun logoutCancellationDoesNotExit() {
        var exits = 0; render { exits++ }
        rule.onNodeWithTag("account_logout").performClick()
        rule.onNodeWithText("Cancelar").performClick()
        rule.runOnIdle { assertEquals(0, exits) }
        rule.onNodeWithTag("account_logout").assertIsDisplayed()
    }
    @Test fun switchRequiresConfirmation() {
        var exits = 0; render { exits++ }
        rule.onNodeWithTag("account_switch").performClick()
        rule.runOnIdle { assertEquals(0, exits) }
        rule.onNodeWithText("Confirmar").performClick()
        rule.runOnIdle { assertEquals(1, exits) }
    }
    @Test fun logoutRequiresConfirmation() {
        var exits = 0; render { exits++ }
        rule.onNodeWithTag("account_logout").performClick()
        rule.runOnIdle { assertEquals(0, exits) }
        rule.onNodeWithText("Confirmar").performClick()
        rule.runOnIdle { assertEquals(1, exits) }
    }
}
