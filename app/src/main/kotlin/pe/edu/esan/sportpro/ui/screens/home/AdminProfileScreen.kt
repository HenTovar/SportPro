package pe.edu.esan.sportpro.ui.screens.home

import pe.edu.esan.sportpro.ui.navigation.safeBack

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
import pe.edu.esan.sportpro.data.model.RolePolicy
import pe.edu.esan.sportpro.data.model.UserRole
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
    var message by remember { mutableStateOf("") }
    var role by remember { mutableStateOf<UserRole?>(null) }
    var target by remember { mutableStateOf(ProfileRoleUi.initialTarget) }
    var roleLoading by remember { mutableStateOf(true) }
    var roleLoadError by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        try {
            val uid = checkNotNull(auth.currentUser).uid
            role = UserRole.valueOf(db.collection("users").document(uid).get().await().getString("role").orEmpty())
        } catch (e: Exception) { roleLoadError = true; message = "No se pudo cargar tu rol." }
        finally { roleLoading = false }
    }
    var requested by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Perfil", style = MaterialTheme.typography.headlineMedium)
        pe.edu.esan.sportpro.features.AccountControls()
        Button(onClick = { navController.navigate("myData") }) { Text("Mis datos") }
        HorizontalDivider()
        pe.edu.esan.sportpro.features.FeaturePreferences(auth.currentUser?.uid.orEmpty())
        Text(auth.currentUser?.email.orEmpty())
        ProfileRoleSummary(role, roleLoading, roleLoadError, creator)
        if (role == UserRole.ADMIN) pe.edu.esan.sportpro.ui.navigation.CoachAssignments()
        if (creator || role == UserRole.ADMIN) {
            Button(onClick = { navController.navigate("adminRequests") }) { Text("Solicitudes de rol") }
        }
        RoleRequestControls(role, target, roleLoading, roleLoadError, busy, requested,
            onTarget = { target = it }, onRequest = {
                busy = true
                scope.launch {
                    try {
                        val user = checkNotNull(auth.currentUser) { "Inicia sesión" }
                        db.collection("roleRequests").document(user.uid).set(mapOf(
                            "uid" to user.uid, "email" to checkNotNull(user.email),
                            "fromRole" to checkNotNull(role).name, "toRole" to checkNotNull(target).name,
                            "createdAt" to FieldValue.serverTimestamp(), "status" to "PENDING"
                        )).await()
                        requested = true
                        message = "Solicitud enviada. Tu rol no cambió."
                    } catch (e: Exception) {
                        message = "No se pudo enviar. Puede existir una solicitud previa; no se reemplaza."
                    } finally { busy = false }
                }
            })
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
        OutlinedButton(onClick = { navController.safeBack() }) { Text("Volver") }
    }
}

private data class AdminRequest(val uid: String, val email: String, val status: String, val fromRole: String, val toRole: UserRole)

@Composable
fun AdminRequestsScreen(navController: NavHostController) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()
    var actorRole by remember { mutableStateOf<UserRole?>(null) }
    var rows by remember { mutableStateOf(emptyList<AdminRequest>()) }
    var busy by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(refresh) {
        busy = true
        try {
            val user = checkNotNull(auth.currentUser)
            val raw = db.collection("users").document(user.uid).get().await().getString("role")
            actorRole = UserRole.values().firstOrNull { it.name == raw }
            check(actorRole == UserRole.ADMIN || isCreator(user)) { "La cola global de roles requiere administrador" }
            val targets = UserRole.values().filter { RolePolicy.canApprove(actorRole, isCreator(user), it) }
            check(targets.isNotEmpty())
            rows = db.collection("roleRequests").whereIn("toRole", targets.map { it.name }).get().await().documents.map {
                AdminRequest(it.id, it.getString("email").orEmpty(), it.getString("status").orEmpty(),
                    it.getString("fromRole").orEmpty(), UserRole.valueOf(it.getString("toRole").orEmpty()))
            }.sortedBy { it.email }
        } catch (e: Exception) { rows = emptyList(); message = "No se pudieron leer las solicitudes: permisos insuficientes o sesión desactualizada." }
        finally { busy = false }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Solicitudes de rol", style = MaterialTheme.typography.headlineMedium)
        if (busy) CircularProgressIndicator()
        Text(message)
        rows.filter { RolePolicy.canApprove(actorRole, isCreator(auth.currentUser), it.toRole) }.forEach { item ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                Text(item.email)
                Text("${item.fromRole} → ${item.toRole.name}: ${item.status}")
                if (item.status == "PENDING") Row {
                    listOf(true, false).forEach { approve ->
                        TextButton(enabled = !busy && item.uid != auth.currentUser?.uid, onClick = {
                            busy = true
                            scope.launch {
                                try {
                                    check(RolePolicy.canApprove(actorRole, isCreator(auth.currentUser), item.toRole))
                                    val request = db.collection("roleRequests").document(item.uid)
                                    db.runTransaction { tx ->
                                        check(tx.get(request).getString("status") == "PENDING") { "Solicitud ya resuelta" }
                                        if (approve) tx.update(db.collection("users").document(item.uid),
                                            RolePolicy.approvalFields(item.toRole))
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
        OutlinedButton(onClick = { navController.safeBack() }) { Text("Volver") }
    }
}
