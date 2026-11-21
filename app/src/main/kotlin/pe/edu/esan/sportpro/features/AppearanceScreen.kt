package pe.edu.esan.sportpro.features

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.edu.esan.sportpro.ui.common.BackButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(uid: String, onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Pantalla y letra") }, navigationIcon = { BackButton(onBack) })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
            .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Elige el aspecto y el tamaño de letra. Se guardan en este dispositivo.")
            FeaturePreferences(uid)
        }
    }
}
