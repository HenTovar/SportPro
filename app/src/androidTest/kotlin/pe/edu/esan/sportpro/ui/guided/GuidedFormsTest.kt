package pe.edu.esan.sportpro.ui.guided

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import pe.edu.esan.sportpro.data.model.UserRole

class GuidedFormsTest {
    @get:Rule val rule = createComposeRule()
    @Test fun parentFormHasNoPlayerOrCoachFields() {
        rule.setContent { MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            GuidedDetailsForm(GuidedFormDraft(role = UserRole.PADRE), FormAccess.REGISTRATION, false, emptyMap(), {})
        } } }
        rule.onNodeWithTag("guided_NAME").assertExists()
        rule.onNodeWithTag("guided_PHONE").assertExists()
        rule.onNodeWithTag("guided_BIRTH_DATE").assertDoesNotExist()
        rule.onNodeWithTag("guided_SPECIALTY").assertDoesNotExist()
    }
    @Test fun coachFormShowsPendingApprovalAndAcceptsExperienceZero() {
        rule.setContent { MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            GuidedDetailsForm(GuidedFormDraft(role = UserRole.ENTRENADOR, yearsExperience = "0"), FormAccess.REGISTRATION, false, emptyMap(), {})
        } } }
        rule.onNodeWithTag("guided_EXPERIENCE").assertTextContains("0")
        rule.onNodeWithText("Pendiente de aprobación:", substring = true).assertExists()
        rule.onNodeWithTag("guided_BIRTH_DATE").assertDoesNotExist()
    }
    @Test fun invalidRegistrationNeverCallsGateway() {
        var calls = 0
        rule.setContent { MaterialTheme { GuidedRegistrationContent(false, null, { _, _, _ -> calls++ }, {}) } }
        rule.onNodeWithTag("guided_register").performScrollTo().performClick()
        rule.onNodeWithText("Elige Jugador, Padre/tutor o Entrenador.").assertExists()
        rule.runOnIdle { assertEquals(0, calls) }
    }
    @Test fun selfCannotEditSportsByClickingFoot() {
        var observed = GuidedFormDraft(role = UserRole.JUGADOR, foot = FootChoice.LEFT)
        rule.setContent { MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            GuidedDetailsForm(observed, FormAccess.SELF, false, emptyMap(), { observed = it })
        } } }
        rule.onNodeWithTag("guided_foot_RIGHT").performScrollTo().assertIsNotEnabled()
        rule.runOnIdle { assertEquals(FootChoice.LEFT, observed.foot) }
    }
    @Test fun leaveRequiresConfirmationAndPassesDisplayedTeam() {
        val left = mutableListOf<String>()
        rule.setContent { MaterialTheme { Column {
            TeamMembershipControls("team-a", "Equipo de prueba", false, {}, { left += it })
        } } }
        rule.onNodeWithTag("team_leave").performClick()
        rule.runOnIdle { assertTrue(left.isEmpty()) }
        rule.onNodeWithTag("team_leave_confirm").performClick()
        rule.runOnIdle { assertEquals(listOf("team-a"), left) }
    }
    @Test fun joinNeedsCodeAndDoesNotAcceptAnotherPlayersUid() {
        val joined = mutableListOf<String>()
        rule.setContent { MaterialTheme { Column {
            TeamMembershipControls(null, null, false, { joined += it }, {})
        } } }
        rule.onNodeWithTag("team_join_submit").assertIsNotEnabled()
        rule.onNodeWithTag("team_join_code").performTextInput("TEAMTEST")
        rule.onNodeWithTag("team_join_submit").assertIsEnabled().performClick()
        rule.runOnIdle { assertEquals(listOf("TEAMTEST"), joined) }
    }
}
