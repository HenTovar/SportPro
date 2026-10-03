@file:Suppress("EXPERIMENTAL_MATERIAL3_API")

package pe.edu.esan.sportpro.ui.screens.players

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.FieldValue
import java.text.ParsePosition
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.Player
import pe.edu.esan.sportpro.data.model.PlayerPosition
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CreatePlayerScreen(teamId: String, navController: NavHostController, playerId: String? = null) {
    var name by remember { mutableStateOf("") }
    var position by remember { mutableStateOf(PlayerPosition.DELANTERO) }
    var birthDate by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var userUid by remember { mutableStateOf("") }
    var parentUid by remember { mutableStateOf("") }
    var positionExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var isUploadingPhoto by remember { mutableStateOf(false) }

    var formReady by remember(playerId) { mutableStateOf(false) }
    var checkingAccess by remember(playerId) { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    LaunchedEffect(teamId, playerId) {
        try {
            requireStaff(firestore)
            if (playerId != null) {
                val document = firestore.collection("teams").document(teamId)
                    .collection("players").document(playerId).get().await()
                check(document.exists()) { "El jugador ya no existe" }
                val player = checkNotNull(document.toObject(Player::class.java))
                name = player.name
                position = player.position
                birthDate = if (player.dateOfBirth == 0L) "" else
                    SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(player.dateOfBirth))
                height = player.height.toString()
                weight = player.weight.toString()
                contact = player.phone
                photoUrl = player.photoUrl
                userUid = document.getString("userUid").orEmpty()
                parentUid = player.parentUid.orEmpty()
            }
            formReady = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar el jugador"
        } finally {
            checkingAccess = false
        }
    }
    if (!formReady) {
        FormUnavailable(checkingAccess, error, navController)
        return
    }

    // Picker para seleccionar foto
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            photoUri = uri
            // Iniciar carga a Cloud Storage
            scope.launch {
                isUploadingPhoto = true
                try {
                    requireStaff(firestore)
                    val photoRef = storage.reference.child("players/${UUID.randomUUID()}.jpg")
                    photoRef.putFile(uri).await()
                    photoUrl = photoRef.downloadUrl.await().toString()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    error = "Error: ${e.message}"
                } finally {
                    isUploadingPhoto = false
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = { Text(if (playerId == null) "Crear Jugador" else "Editar Jugador") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Foto del jugador
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (photoUri != null || photoUrl != null) {
                    AsyncImage(
                        model = photoUri ?: photoUrl,
                        contentDescription = "Foto de jugador",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Agregar foto",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Button(
                onClick = {
                    pickImageLauncher.launch("image/*")
                },
                enabled = !isUploadingPhoto && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isUploadingPhoto) "Cargando foto..." else "Seleccionar Foto")
            }

            if (photoUrl != null) {
                Text("Foto cargada exitosamente", style = MaterialTheme.typography.labelSmall)
            }

            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { positionExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Posición: ${position.name.replace('_', ' ')}")
                }
                DropdownMenu(expanded = positionExpanded, onDismissRequest = { positionExpanded = false }) {
                    PlayerPosition.values().forEach { value ->
                        DropdownMenuItem(text = { Text(value.name.replace('_', ' ')) }, onClick = {
                            position = value
                            positionExpanded = false
                        })
                    }
                }
            }

            TextField(
                value = birthDate,
                onValueChange = { birthDate = it },
                label = { Text("Fecha Nacimiento (yyyy-mm-dd)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            TextField(
                value = height,
                onValueChange = { height = it },
                label = { Text("Altura (cm)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            TextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("Peso (kg)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            TextField(
                value = contact,
                onValueChange = { contact = it },
                label = { Text("Contacto") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("Vincular cuentas", style = MaterialTheme.typography.titleSmall)
            Text("Introduce el UID de la cuenta registrada. Solo el personal puede cambiar estos vínculos.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = userUid, onValueChange = { userUid = it },
                label = { Text("UID de la cuenta del jugador (opcional)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = parentUid, onValueChange = { parentUid = it },
                label = { Text("UID de la cuenta del padre (opcional)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            if (error.isNotEmpty()) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = "Nombre requerido"
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        try {
                            requireStaff(firestore)
                            val parsedBirthDate = parseBirthDate(birthDate)
                            val parsedHeight = if (height.isBlank()) 0f else checkNotNull(height.toFloatOrNull()) { "Altura inválida" }
                            val parsedWeight = if (weight.isBlank()) 0f else checkNotNull(weight.toFloatOrNull()) { "Peso inválido" }
                            require(parsedHeight.isFinite() && parsedHeight >= 0f) { "Altura inválida" }
                            require(parsedWeight.isFinite() && parsedWeight >= 0f) { "Peso inválido" }
                            val players = firestore.collection("teams").document(teamId).collection("players")
                            val document = playerId?.let(players::document) ?: players.document()
                            val newUserUid = userUid.trim().takeIf { it.isNotEmpty() }
                            val newParentUid = parentUid.trim().takeIf { it.isNotEmpty() }
                            val fields = mapOf<String, Any?>(
                                "id" to document.id, "teamId" to teamId, "name" to name.trim(),
                                "position" to position.name, "dateOfBirth" to parsedBirthDate,
                                "height" to parsedHeight, "weight" to parsedWeight, "phone" to contact.trim(),
                                "photoUrl" to photoUrl, "userUid" to newUserUid, "parentUid" to newParentUid,
                                "updatedAt" to System.currentTimeMillis()
                            )
                            // Read every affected profile before any write; links and player commit together.
                            firestore.runTransaction { transaction ->
                                val previous = transaction.get(document)
                                check(playerId == null || previous.exists()) { "El jugador ya no existe" }
                                val oldIds = listOfNotNull(previous.getString("userUid"), previous.getString("parentUid"))
                                    .filter { it.isNotBlank() }
                                val targetRoles = listOfNotNull(
                                    newUserUid?.let { it to "JUGADOR" }, newParentUid?.let { it to "PADRE" }
                                )
                                require(newUserUid == null || newUserUid != newParentUid) { "Jugador y padre deben tener cuentas distintas" }
                                val profiles = (oldIds + targetRoles.map { it.first }).distinct().associateWith { uid ->
                                    transaction.get(firestore.collection("users").document(uid))
                                }
                                targetRoles.forEach { (uid, role) ->
                                    val profile = profiles.getValue(uid)
                                    require(profile.exists() && profile.getString("role") == role && profile.getBoolean("active") != false) {
                                        "La cuenta $uid debe existir y tener rol $role activo"
                                    }
                                    val linkedPlayer = profile.getString("playerId")
                                    require(linkedPlayer.isNullOrBlank() ||
                                        (linkedPlayer == document.id && profile.getString("teamId") == teamId)) {
                                        "La cuenta $uid ya está vinculada a otro jugador"
                                    }
                                }
                                oldIds.filter { it != newUserUid && it != newParentUid }.forEach { uid ->
                                    val profile = profiles.getValue(uid)
                                    if (profile.exists() && profile.getString("teamId") == teamId && profile.getString("playerId") == document.id) {
                                        transaction.update(profile.reference, mapOf(
                                            "teamId" to "", "playerId" to ""
                                        ))
                                    }
                                }
                                targetRoles.forEach { (uid, _) ->
                                    transaction.update(profiles.getValue(uid).reference,
                                        mapOf("teamId" to teamId, "playerId" to document.id))
                                }
                                if (playerId == null) {
                                    transaction.set(document, fields + mapOf("createdAt" to System.currentTimeMillis(), "active" to true))
                                } else {
                                    // Partial update preserves number, email, emergency contacts, active and future fields.
                                    transaction.update(document, fields)
                                }
                            }.await()
                            navController.popBackStack()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            error = e.message ?: "Error"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading && !isUploadingPhoto,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "Guardando..." else if (playerId == null) "Crear Jugador" else "Guardar cambios")
            }
        }
    }
}

private suspend fun requireStaff(firestore: FirebaseFirestore): String {
    val uid = checkNotNull(FirebaseAuth.getInstance().currentUser?.uid) { "Inicia sesión para continuar" }
    val profile = firestore.collection("users").document(uid).get().await()
    check(profile.getString("role") in setOf("ADMIN", "ENTRENADOR") && profile.getBoolean("active") != false) {
        "Solo administradores y entrenadores pueden gestionar estos datos"
    }
    return uid
}

@Composable
private fun FormUnavailable(loading: Boolean, error: String, navController: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (loading) CircularProgressIndicator()
        else Text(error, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = { navController.popBackStack() }) { Text("Volver") }
    }
}

private fun parseBirthDate(value: String): Long {
    if (value.isBlank()) return 0L
    require(Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) { "Fecha de nacimiento inválida; usa yyyy-mm-dd" }
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }
    val cursor = ParsePosition(0)
    val parsed = formatter.parse(value, cursor)
    require(parsed != null && cursor.index == value.length) { "Fecha de nacimiento inválida" }
    require(parsed.time <= System.currentTimeMillis()) { "La fecha de nacimiento no puede ser futura" }
    return parsed.time
}
