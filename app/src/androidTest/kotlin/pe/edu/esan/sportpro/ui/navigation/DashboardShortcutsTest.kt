package pe.edu.esan.sportpro.ui.navigation

import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pe.edu.esan.sportpro.data.model.*
import pe.edu.esan.sportpro.ui.screens.home.HomeDashboard
import pe.edu.esan.sportpro.ui.theme.SportProTheme

/** Offline fixtures: verifies dashboard touch->route, not a Firestore write. */
class DashboardShortcutsTest {
    @get:Rule val rule = createComposeRule()
    private val team = Team(id="team",name="Academia")
    private val training = Training(id="training",teamId="team",name="Sesión",date=Long.MAX_VALUE-1000)
    private val match = LiveMatch(id="match",name="Jornada",status="in_progress",teamA="team")
    private var destination = ""
    private fun render(role:UserRole) {
        val user = User(uid="fixture",name="Alex",role=role,teamId="team",playerId="player")
        rule.setContent {
            SportProTheme {
                CompositionLocalProvider(LocalTeamSession provides TeamSession(user,listOf(team),team,false)) {
                    Surface { HomeDashboard(user,false,null,onNavigate={destination=it},selectedTeam=team,matches=listOf(match),trainings=listOf(training)) }
                }
            }
        }
    }
    @Test fun staffAttendanceIsOneDashboardTap() {
        render(UserRole.ENTRENADOR)
        rule.onNodeWithText("Registrar asistencia").performScrollTo().performClick()
        rule.runOnIdle { assertEquals("attendance/team/training",destination) }
    }
    @Test fun staffEventEditorIsOneDashboardTap() {
        render(UserRole.ENTRENADOR)
        rule.onNodeWithText("Registrar evento").performScrollTo().performClick()
        rule.runOnIdle { assertEquals("liveMatch/match?event=true",destination) }
    }
    @Test fun playerAttendanceIsOneDashboardTapAndHasNoStaffAction() {
        render(UserRole.JUGADOR)
        rule.onNodeWithText("Registrar asistencia").assertDoesNotExist()
        rule.onNodeWithText("Mi asistencia").performScrollTo().performClick()
        rule.runOnIdle { assertEquals("attendanceHistory/team/player",destination) }
    }
    @Test fun parentWallIsOneDashboardTap() {
        render(UserRole.PADRE)
        rule.onNodeWithText("Muro del equipo").performScrollTo().performClick()
        rule.runOnIdle { assertEquals("community/team",destination) }
    }
    @Test fun liveMatchIsOneDashboardTap() {
        render(UserRole.JUGADOR)
        rule.onNodeWithText("EN VIVO · Jornada").performScrollTo().performClick()
        rule.runOnIdle { assertEquals("liveMatch/match?event=false",destination) }
    }
}
