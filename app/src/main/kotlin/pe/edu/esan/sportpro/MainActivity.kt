package pe.edu.esan.sportpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.firebase.auth.FirebaseAuth
import pe.edu.esan.sportpro.ui.theme.SportProTheme
import pe.edu.esan.sportpro.ui.navigation.SportProNavHost

/**
 * Actividad principal de SportPro.
 * Configura el tema y la navegación de la aplicación.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SportProTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val startDestination = if (currentUser != null) {
                        "home"
                    } else {
                        "login"
                    }
                    SportProNavHost(startDestination = startDestination)
                }
            }
        }
    }
}
