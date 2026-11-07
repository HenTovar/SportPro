package pe.edu.esan.sportpro.ui.guided

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TeamCodeCopyTest {
    @get:Rule val rule = createComposeRule()
    private class RecordingClipboard : ClipboardManager {
        var storedText: AnnotatedString? = null
        override fun setText(annotatedString: AnnotatedString) { storedText = annotatedString }
        override fun getText(): AnnotatedString? = storedText
    }
    @Test fun clickCopiesExactDisplayedTeamCodeAndConfirms() {
        val clipboard = RecordingClipboard()
        var confirmations = 0
        rule.setContent { CompositionLocalProvider(LocalClipboardManager provides clipboard) {
            MaterialTheme { TeamCodeDisplay("TEAM22222222", true, false) { confirmations++ } }
        } }
        rule.onNodeWithText("TEAM22222222").assertExists()
        rule.onNodeWithTag("team_code_copy").assertIsEnabled().performClick()
        rule.runOnIdle {
            assertEquals("TEAM22222222", clipboard.storedText?.text)
            assertEquals(1, confirmations)
        }
    }
    @Test fun unauthorizedCannotSeeOrCopyCode() {
        rule.setContent { MaterialTheme { TeamCodeDisplay("TEAM22222222", false, false) {} } }
        rule.onNodeWithText("TEAM22222222").assertDoesNotExist()
        rule.onNodeWithTag("team_code_copy").assertDoesNotExist()
    }
    @Test fun absentCodeHasNoCopyButton() {
        rule.setContent { MaterialTheme { TeamCodeDisplay(null, true, false) {} } }
        rule.onNodeWithTag("team_code_copy").assertDoesNotExist()
    }
    @Test fun blankCodeHasNoCopyButton() {
        rule.setContent { MaterialTheme { TeamCodeDisplay("  ", true, false) {} } }
        rule.onNodeWithTag("team_code_copy").assertDoesNotExist()
    }
    @Test fun busyDisablesCopy() {
        rule.setContent { MaterialTheme { TeamCodeDisplay("TEAM22222222", true, true) {} } }
        rule.onNodeWithTag("team_code_copy").assertIsNotEnabled()
    }
}
