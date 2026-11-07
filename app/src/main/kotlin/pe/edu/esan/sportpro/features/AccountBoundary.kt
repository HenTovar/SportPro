package pe.edu.esan.sportpro.features

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import coil.imageLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class AccountActions(val exit: () -> Unit = {})
val LocalAccountActions = staticCompositionLocalOf { AccountActions() }

/** Owns the lifetime of ALL account screens, their view models and snapshot listeners. */
@Composable
fun AccountBoundary(content: @Composable (generation: Int, resetToLogin: Boolean) -> Unit) {
    val context = LocalContext.current.applicationContext
    val auth = remember { FirebaseAuth.getInstance() }
    val prefs = remember { context.getSharedPreferences("account_boundary", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var uid by remember { mutableStateOf(auth.currentUser?.uid.orEmpty()) }
    var knownUid by remember { mutableStateOf(prefs.getString("uid", uid).orEmpty()) }
    var pending by remember { mutableStateOf(prefs.getBoolean("cleanup_pending", false)) }
    var running by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var generation by remember { mutableIntStateOf(prefs.getInt("generation", 0)) }
    var resetToLogin by remember { mutableStateOf(false) }
    // Keep the terminated instance if clearPersistence fails, so Retry clears THAT store.
    var retiringDb by remember { mutableStateOf<FirebaseFirestore?>(null) }
    var terminated by remember { mutableStateOf(false) }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { uid = it.currentUser?.uid.orEmpty() }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }
    fun cleanup() {
        if (running) return
        // Synchronous lock: no second click or account route can run before composition leaves.
        pending = true; running = true; error = false
        scope.launch {
            try {
                AccountExit.run(object : AccountExitSteps {
                    override suspend fun markPending() {
                        check(prefs.edit().putBoolean("cleanup_pending", true).commit())
                    }
                    override suspend fun detachScreens() {
                        // The pending branch removes the NavHost, ViewModels and effect scopes.
                        withFrameNanos { }; withFrameNanos { }
                    }
                    override suspend fun signOut() { auth.signOut() }
                    override suspend fun clearSelectionsAndNotifications() {
                        check(context.getSharedPreferences("navigation", Context.MODE_PRIVATE).edit().clear().commit())
                        val appearance = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
                        val edit = appearance.edit()
                        appearance.all.keys.filter { it.startsWith("notifications_") }.forEach { edit.remove(it) }
                        check(edit.commit())
                        LocalNotifier(context).clear()
                        context.imageLoader.memoryCache?.clear()
                        withContext(Dispatchers.IO) { context.imageLoader.diskCache?.clear() }
                    }
                    override suspend fun terminateAndClearCache() {
                        val db = retiringDb ?: FirebaseFirestore.getInstance().also { retiringDb = it }
                        if (!terminated) { db.terminate().await(); terminated = true }
                        db.clearPersistence().await()
                        FirestoreOffline.restartAfterClear()
                    }
                    override suspend fun finish() {
                        check(prefs.edit().putBoolean("cleanup_pending", false).putString("uid", "")
                            .putInt("generation", generation + 1).commit())
                        knownUid = ""; uid = ""; generation++; resetToLogin = true
                        retiringDb = null; terminated = false; pending = false
                    }
                })
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = true } // Pending survives process death and denies login.
            finally { running = false }
        }
    }
    val changedAccount = knownUid.isNotBlank() && uid != knownUid
    LaunchedEffect(uid, knownUid) {
        if (pending || changedAccount) cleanup()
        else if (uid.isNotBlank()) {
            if (prefs.edit().putString("uid", uid).commit()) knownUid = uid
            else cleanup()
        }
    }
    if (pending || changedAccount) {
        BackHandler { } // Neither the old stack nor another account is accessible during cleanup.
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Cerrando sesión", style = MaterialTheme.typography.headlineSmall)
            if (error) {
                Text("No se pudo terminar la limpieza del dispositivo. Reintenta antes de iniciar otra sesión.")
                Button(enabled = !running, onClick = { cleanup() }) { Text("Reintentar limpieza") }
            } else CircularProgressIndicator()
        }
    } else {
        CompositionLocalProvider(LocalAccountActions provides AccountActions { cleanup() }) {
            key(generation) { content(generation, resetToLogin) }
        }
    }
}

@Composable
fun AccountControls() {
    val exit = LocalAccountActions.current.exit
    var confirmation by remember { mutableStateOf<String?>(null) }
    Text("Cuenta", style = MaterialTheme.typography.titleLarge)
    Button(onClick = { confirmation = "Cerrar sesión" }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("account_logout")) {
        Text("Cerrar sesión")
    }
    OutlinedButton(onClick = { confirmation = "Cambiar de cuenta" }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("account_switch")) {
        Text("Cambiar de cuenta")
    }
    confirmation?.let { title ->
        AlertDialog(onDismissRequest = { confirmation = null }, title = { Text(title) },
            text = { Text("Se cerrará tu sesión y se borrarán los datos locales y las notificaciones de este dispositivo. Los cambios sin sincronizar se perderán. Después podrás entrar con otra cuenta.") },
            confirmButton = { TextButton(onClick = { confirmation = null; exit() }) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Cancelar") } })
    }
}
