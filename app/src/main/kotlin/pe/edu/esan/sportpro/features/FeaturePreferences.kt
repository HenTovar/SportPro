package pe.edu.esan.sportpro.features

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun FeaturePreferences(uid: String) {
    val context=LocalContext.current
    val prefs=remember { context.getSharedPreferences("appearance",Context.MODE_PRIVATE) }
    var theme by remember { mutableStateOf(prefs.getString("theme","system")?:"system") }
    var font by remember { mutableFloatStateOf(prefs.getFloat("font",1f)) }
    var enabled by remember(uid) { mutableStateOf(prefs.getBoolean("notifications_$uid",false)) }
    var permissionMessage by remember { mutableStateOf("") }
    val request=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        enabled=granted;prefs.edit().putBoolean("notifications_$uid",granted).apply()
        permissionMessage=if(granted)"Avisos locales habilitados" else "Permiso denegado. Puedes seguir usando toda la app sin avisos."
    }
    Text("Pantalla y avisos",style=MaterialTheme.typography.titleLarge)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf("system" to "Sistema","light" to "Claro","dark" to "Oscuro").forEach { (key,label) ->
            FilterChip(selected=theme==key,onClick={theme=key;prefs.edit().putString("theme",key).apply()},label={Text(label)},modifier=Modifier.heightIn(min=48.dp))
        }
    }
    Text("Letra (respeta si el sistema ya usa un tamaño mayor)")
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf(1f to "Sistema",1.3f to "Grande 130%",1.5f to "Muy grande 150%").forEach { (value,label) ->
            FilterChip(selected=font==value,onClick={font=value;prefs.edit().putFloat("font",value).apply()},label={Text(label)},modifier=Modifier.heightIn(min=48.dp))
        }
    }
    if(uid.isNotBlank()) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("Avisos locales",Modifier.weight(1f))
            Switch(checked=enabled,onCheckedChange={on ->
                if(on && Build.VERSION.SDK_INT>=33)request.launch(Manifest.permission.POST_NOTIFICATIONS)
                else {enabled=on;prefs.edit().putBoolean("notifications_$uid",on).apply()}
            })
        }
        Text("Inicio de partido, goles y cambios de entrenamiento del equipo seleccionado. Sólo mientras el listener siga activo: la app abierta o en segundo plano. Con la app cerrada no se garantiza ningún aviso.",style=MaterialTheme.typography.bodySmall)
        if(permissionMessage.isNotBlank())Text(permissionMessage)
    }
}
