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
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.Player
import pe.edu.esan.sportpro.data.model.PlayerPosition
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CreatePlayerScreen(teamId: String, navController: NavHostController) {
    var name by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var isUploadingPhoto by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

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
                    val tempPlayerId = UUID.randomUUID().toString()
                    val photoRef = storage.reference.child("players/$tempPlayerId.jpg")
                    photoRef.putFile(uri).addOnSuccessListener {
                        photoRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                            photoUrl = downloadUrl.toString()
                            isUploadingPhoto = false
                        }.addOnFailureListener { e ->
                            error = "Error al obtener URL de foto: ${e.message}"
                            isUploadingPhoto = false
                        }
                    }.addOnFailureListener { e ->
                        error = "Error al cargar foto: ${e.message}"
                        isUploadingPhoto = false
                    }
                } catch (e: Exception) {
                    error = "Error: ${e.message}"
                    isUploadingPhoto = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Jugador") },
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
                if (photoUri != null) {
                    AsyncImage(
                        model = photoUri,
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
                enabled = !isUploadingPhoto,
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

            TextField(
                value = position,
                onValueChange = { position = it },
                label = { Text("Posición") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

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
                            // Parse birth date string to timestamp
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val parsedBirthDate = try {
                                dateFormat.parse(birthDate)?.time ?: 0L
                            } catch (e: Exception) {
                                0L
                            }

                            val document = firestore.collection("teams").document(teamId)
                                .collection("players").document()
                            val player = Player(
                                id = document.id,
                                teamId = teamId,
                                name = name,
                                position = PlayerPosition.values().firstOrNull { it.name == position.uppercase().replace(" ", "_") } ?: PlayerPosition.DELANTERO,
                                dateOfBirth = parsedBirthDate,
                                height = height.toFloatOrNull() ?: 0f,
                                weight = weight.toFloatOrNull() ?: 0f,
                                phone = contact,
                                photoUrl = photoUrl,
                                createdAt = System.currentTimeMillis()
                            )
                            document.set(player)
                                .addOnSuccessListener {
                                    isLoading = false
                                    navController.popBackStack()
                                }
                                .addOnFailureListener { e ->
                                    error = e.message ?: "Error"
                                    isLoading = false
                                }
                        } catch (e: Exception) {
                            error = e.message ?: "Error"
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "Guardando..." else "Crear Jugador")
            }
        }
    }
}
