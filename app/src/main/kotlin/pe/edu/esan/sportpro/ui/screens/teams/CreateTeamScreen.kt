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
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.Team
import pe.edu.esan.sportpro.data.model.TeamCategory

@Composable
fun CreateTeamScreen(navController: NavHostController) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TeamCategory.SUB_15) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Equipo") },
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
                            val document = firestore.collection("teams").document()
                            val team = Team(
                                id = document.id,
                                name = name,
                                category = category,
                                createdAt = System.currentTimeMillis()
                            )
                            document.set(team)
                                .addOnSuccessListener {
                                    isLoading = false
                                    navController.popBackStack()
                                }
                                .addOnFailureListener { e ->
                                    error = e.message ?: "Error al crear equipo"
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
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Crear Equipo")
                }
            }
        }
    }
}
