package pe.edu.esan.sportpro.ui.guided

import org.junit.Assert.*
import org.junit.Test
import pe.edu.esan.sportpro.data.model.User
import pe.edu.esan.sportpro.data.model.UserRole

class GuidedFamilyCodeAccessTest {
    private val admin = User(uid = "admin", role = UserRole.ADMIN, isActive = true)
    @Test fun activeAdminWithLoadedPlayerCanIssue() { assertTrue(canIssueFamilyCodeFromEditor(admin, "team", "player", true, false)) }
    @Test fun inactiveAdminCannotIssue() { assertFalse(canIssueFamilyCodeFromEditor(admin.copy(isActive = false), "team", "player", true, false)) }
    @Test fun coachCannotIssueFromAdminEditor() { assertFalse(canIssueFamilyCodeFromEditor(admin.copy(role = UserRole.ENTRENADOR, coachTeamIds = listOf("team")), "team", "player", true, false)) }
    @Test fun playerCannotIssue() { assertFalse(canIssueFamilyCodeFromEditor(admin.copy(role = UserRole.JUGADOR), "team", "player", true, false)) }
    @Test fun parentCannotIssue() { assertFalse(canIssueFamilyCodeFromEditor(admin.copy(role = UserRole.PADRE), "team", "player", true, false)) }
    @Test fun missingSessionCannotIssue() { assertFalse(canIssueFamilyCodeFromEditor(null, "team", "player", true, false)) }
    @Test fun blankTeamCannotIssue() { assertFalse(canIssueFamilyCodeFromEditor(admin, " ", "player", true, false)) }
    @Test fun blankPlayerCannotIssue() { assertFalse(canIssueFamilyCodeFromEditor(admin, "team", " ", true, false)) }
    @Test fun loadingPlayerCannotIssue() { assertFalse(canIssueFamilyCodeFromEditor(admin, "team", "player", false, false)) }
    @Test fun savingPlayerCannotIssue() { assertFalse(canIssueFamilyCodeFromEditor(admin, "team", "player", true, true)) }
}
