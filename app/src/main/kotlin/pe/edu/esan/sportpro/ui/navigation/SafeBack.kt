package pe.edu.esan.sportpro.ui.navigation

import androidx.navigation.NavController

/** Keep a destination visible even when Back is tapped again at the root. */
fun NavController.safeBack() {
    if (previousBackStackEntry != null) {
        popBackStack()
    } else {
        navigate("home") { launchSingleTop = true }
    }
}
