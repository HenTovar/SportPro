package pe.edu.esan.sportpro.ui.screens.home

import org.junit.Assert.*
import org.junit.Test
import pe.edu.esan.sportpro.data.model.UserRole

class ProfileRoleUiTest {
    @Test fun playerStartsWithoutAnyRequestedRoleAndKeepsActualRole() {
        val target = ProfileRoleUi.initialTarget
        assertNull(target)
        assertTrue(UserRole.values().none { it == target })
        assertEquals("Rol: Jugador", ProfileRoleUi.roleText(UserRole.JUGADOR, false, false))
        assertFalse(ProfileRoleUi.canRequest(UserRole.JUGADOR, target, false, false, false, false))
    }
    @Test fun loadingRoleIsNeverReportedAsSignedOut() {
        assertEquals("Rol: Cargando…", ProfileRoleUi.roleText(null, true, false))
        assertFalse(ProfileRoleUi.canRequest(null, UserRole.ADMIN, true, false, false, false))
    }
    @Test fun failedRoleLoadIsExplicitAndCannotSubmit() {
        assertEquals("Rol: No disponible", ProfileRoleUi.roleText(null, false, true))
        assertFalse(ProfileRoleUi.canRequest(null, UserRole.ADMIN, false, true, false, false))
    }
    @Test fun selectingRequestDoesNotChangeDisplayedActualRole() {
        assertTrue(ProfileRoleUi.canRequest(UserRole.JUGADOR, UserRole.PADRE, false, false, false, false))
        assertEquals("Rol: Jugador", ProfileRoleUi.roleText(UserRole.JUGADOR, false, false))
        assertFalse(ProfileRoleUi.canRequest(UserRole.JUGADOR, UserRole.JUGADOR, false, false, false, false))
    }
    @Test fun busyOrAlreadyRequestedStaysDisabled() {
        assertFalse(ProfileRoleUi.canRequest(UserRole.JUGADOR, UserRole.ADMIN, false, false, true, false))
        assertFalse(ProfileRoleUi.canRequest(UserRole.JUGADOR, UserRole.ADMIN, false, false, false, true))
    }
}
