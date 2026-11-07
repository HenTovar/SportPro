package pe.edu.esan.sportpro.features

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AccountExitTest {
    private class Steps(private val failAt: String? = null) : AccountExitSteps {
        val calls = mutableListOf<String>()
        var pending = false
        var loginAllowed = false
        var oldScreen = true
        var listeners = 3
        var selectedTeam: String? = "team-a"
        var selectedChild: String? = "child-a"
        var cache = mutableListOf("private-a")
        var notifications = 2
        private fun step(name: String) { calls += name; if (failAt == name) error("test failure") }
        override suspend fun markPending() { step("mark"); pending = true }
        override suspend fun detachScreens() { step("detach"); oldScreen = false; listeners = 0 }
        override suspend fun signOut() { step("signOut"); assertFalse(oldScreen); assertEquals(0, listeners) }
        override suspend fun clearSelectionsAndNotifications() { step("local"); selectedTeam = null; selectedChild = null; notifications = 0 }
        override suspend fun terminateAndClearCache() { step("cache"); cache.clear() }
        override suspend fun finish() { step("finish"); assertTrue(cache.isEmpty()); pending = false; loginAllowed = true }
    }
    @Test fun logoutClearsBeforeAnotherLogin() = runBlocking {
        val s = Steps(); AccountExit.run(s)
        assertEquals(listOf("mark", "detach", "signOut", "local", "cache", "finish"), s.calls)
        assertNull(s.selectedTeam); assertNull(s.selectedChild); assertEquals(0, s.notifications)
        assertTrue(s.loginAllowed); assertFalse(s.oldScreen); assertFalse(s.pending)
    }
    @Test fun cleanupFailureNeverAdmitsNextAccount() = runBlocking {
        for (at in listOf("detach", "signOut", "local", "cache", "finish")) {
            val s = Steps(at)
            try { AccountExit.run(s); fail("should fail") } catch (_: IllegalStateException) { }
            assertTrue(s.pending); assertFalse(s.loginAllowed)
        }
    }
    @Test fun pendingMarkerFailureDoesNotSignOutOrClear() = runBlocking {
        val s = Steps("mark")
        try { AccountExit.run(s); fail("should fail") } catch (_: IllegalStateException) { }
        assertEquals(listOf("mark"), s.calls); assertFalse(s.loginAllowed)
    }
}
