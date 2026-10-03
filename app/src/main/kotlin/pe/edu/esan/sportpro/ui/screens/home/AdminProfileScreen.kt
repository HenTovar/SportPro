package pe.edu.esan.sportpro.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

internal fun isCreator(user: FirebaseUser?): Boolean =
    user?.email == "harontovar@gmail.com" && user.isEmailVerified

@Composable
fun AdminProfileScreen(navController: NavHostController) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()
    var creator by remember { mutableStateOf(isCreator(auth.currentUser)) }
    var busy by remember { mutableStateOf(false) }
    var requested by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Perfil", style = MaterialTheme.typography.headlineMedium)
        Text(auth.currentUser?.email.orEmpty())
        if (creator) {
            Text("Rol: Creador")
            Button(onClick = { navController.navigate("adminRequests") }) { Text("Solicitudes de admin") }
        } else {
            Text("Solicitar no cambia tu rol. El creador debe aprobar la solicitud.")
            Button(enabled = !busy && !requested, onClick = {
                busy = true
                scope.launch {
                    try {
                        val user = checkNotNull(auth.currentUser) { "Inicia sesión" }
                        db.collection("adminRequests").document(user.uid).set(mapOf(
                            "uid" to user.uid, "email" to checkNotNull(user.email),
                            "createdAt" to FieldValue.serverTimestamp(), "status" to "PENDING"
                        )).await()
                        requested = true
                        message = "Solicitud enviada. Tu rol no cambió."
                    } catch (e: Exception) {
                        message = "No se pudo enviar. Puede existir una solicitud previa; no se reemplaza."
                    } finally { busy = false }
                }
            }) { Text("Solicitar rol de admin") }
        }
        if (auth.currentUser?.email == "harontovar@gmail.com" && !creator) {
            Text("Verifica tu correo en Firebase Auth para habilitar Creador.")
            OutlinedButton(enabled = !busy, onClick = { scope.launch {
                busy = true
                try { auth.currentUser?.sendEmailVerification()?.await(); message = "Correo de verificación enviado." }
                catch (e: Exception) { message = "No se pudo enviar la verificación." }
                finally { busy = false }
            } }) { Text("Enviar verificación") }
            OutlinedButton(enabled = !busy, onClick = { scope.launch {
                busy = true
                try {
                    auth.currentUser?.reload()?.await()
                    auth.currentUser?.getIdToken(true)?.await()
                    creator = isCreator(auth.currentUser)
                    message = if (creator) "Correo verificado." else "El correo todavía no está verificado."
                } catch (e: Exception) { message = "No se pudo actualizar la sesión." }
                finally { busy = false }
            } }) { Text("Ya verifiqué mi correo") }
        }
        if (busy) CircularProgressIndicator()
        Text(message)
        OutlinedButton(onClick = { navController.popBackStack() }) { Text("Volver") }
    }
}

private data class AdminRequest(val uid: String, val email: String, val status: String)

@Composable
fun AdminRequestsScreen(navController: NavHostController) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf(emptyList<AdminRequest>()) }
    var busy by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(refresh) {
        busy = true
        try {
            check(isCreator(auth.currentUser)) { "Sólo el creador verificado" }
            rows = db.collection("adminRequests").get().await().documents.map {
                AdminRequest(it.id, it.getString("email").orEmpty(), it.getString("status").orEmpty())
            }.sortedBy { it.email }
        } catch (e: Exception) { rows = emptyList(); message = "No se pudieron leer las solicitudes: acceso exclusivo del creador verificado." }
        finally { busy = false }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Solicitudes de admin", style = MaterialTheme.typography.headlineMedium)
        if (busy) CircularProgressIndicator()
        Text(message)
        if (isCreator(auth.currentUser)) rows.forEach { item ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                Text(item.email)
                Text(item.status)
                if (item.status == "PENDING") Row {
                    listOf(true, false).forEach { approve ->
                        TextButton(enabled = !busy && item.uid != auth.currentUser?.uid, onClick = {
                            busy = true
                            scope.launch {
                                try {
                                    check(isCreator(auth.currentUser))
                                    val request = db.collection("adminRequests").document(item.uid)
                                    db.runTransaction { tx ->
                                        check(tx.get(request).getString("status") == "PENDING") { "Solicitud ya resuelta" }
                                        if (approve) tx.update(db.collection("users").document(item.uid), "role", "ADMIN")
                                        tx.update(request, "status", if (approve) "APPROVED" else "REJECTED")
                                    }.await()
                                    message = if (approve) "Solicitud aprobada." else "Solicitud rechazada."
                                    refresh++
                                } catch (e: Exception) { message = "No se pudo resolver la solicitud; no se confirmó ningún cambio." }
                                finally { busy = false }
                            }
                        }) { Text(if (approve) "Aprobar" else "Rechazar") }
                    }
                }
            } }
        }
        OutlinedButton(enabled = !busy, onClick = { refresh++ }) { Text("Actualizar") }
        OutlinedButton(onClick = { navController.popBackStack() }) { Text("Volver") }
    }
}
