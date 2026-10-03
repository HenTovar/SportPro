@file:Suppress("EXPERIMENTAL_MATERIAL3_API")

package pe.edu.esan.sportpro.ui.screens.teams

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException

import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.Team
import pe.edu.esan.sportpro.data.model.TeamCategory

@Composable
fun CreateTeamScreen(navController: NavHostController, teamId: String? = null) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TeamCategory.SUB_15) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var formReady by remember(teamId) { mutableStateOf(false) }
    var checkingAccess by remember(teamId) { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(teamId) {
        try {
            requireStaff(firestore)
            if (teamId != null) {
                val document = firestore.collection("teams").document(teamId).get().await()
                check(document.exists()) { "El equipo ya no existe" }
                val team = checkNotNull(document.toObject(Team::class.java))
                name = team.name
                category = team.category
            }
            formReady = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar el equipo"
        } finally {
            checkingAccess = false
        }
    }
    if (!formReady) {
        FormUnavailable(checkingAccess, error, navController)
        return
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = { Text(if (teamId == null) "Crear Equipo" else "Editar Equipo") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextField(
                value = name,
                onValueChange = { name = it; error = "" },
                label = { Text("Nombre del Equipo") },
                modifier = Modifier.fillMaxWidth(),
                isError = error.contains("Nombre"),
                singleLine = true
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { categoryDropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Categoría: ${category.name}")
                }
                DropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TeamCategory.values().forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = {
                                category = cat
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

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
                            val uid = requireStaff(firestore)
                            val collection = firestore.collection("teams")
                            val document = teamId?.let(collection::document) ?: collection.document()
                            if (teamId == null) {
                                document.set(Team(
                                    id = document.id,
                                    name = name.trim(),
                                    category = category,
                                    ownerId = uid,
                                    createdAt = System.currentTimeMillis()
                                )).await()
                            } else {
                                document.update(mapOf(
                                    "id" to document.id,
                                    "name" to name.trim(),
                                    "category" to category.name,
                                    "updatedAt" to System.currentTimeMillis()
                                )).await()
                            }
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
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (teamId == null) "Crear Equipo" else "Guardar cambios")
                }
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
