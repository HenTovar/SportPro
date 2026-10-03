package pe.edu.esan.sportpro.ui.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.*

class SafeBackTest {
    @Test
    fun rootAndRepeatedBackNeverPopTheLastDestination() {
        val nav = mock<NavController>()
        whenever(nav.previousBackStackEntry).thenReturn(null)
        nav.safeBack()
        nav.safeBack()
        verify(nav, never()).popBackStack()
        val options = argumentCaptor<NavOptionsBuilder.() -> Unit>()
        verify(nav, times(2)).navigate(eq("home"), options.capture())
        options.allValues.forEach { configure ->
            val builder = NavOptionsBuilder().apply(configure)
            assertTrue(builder.launchSingleTop)
        }
    }

    @Test
    fun backAfterSaveKeepsHomeInsteadOfPoppingIt() {
        val nav = mock<NavController>()
        val home = mock<NavBackStackEntry>()
        whenever(nav.previousBackStackEntry).thenReturn(home, null)
        whenever(nav.popBackStack()).thenReturn(true)
        nav.safeBack() // save returns to Home
        nav.safeBack() // delayed/repeated Back at Home
        verify(nav, times(1)).popBackStack()
        verify(nav).navigate(eq("home"), any<NavOptionsBuilder.() -> Unit>())
    }
}
