@file:Suppress("EXPERIMENTAL_MATERIAL3_API")

package pe.edu.esan.sportpro.ui.screens.trainings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.Exercise
import pe.edu.esan.sportpro.data.model.ExerciseDifficulty
import pe.edu.esan.sportpro.data.model.Training
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CreateTrainingScreen(teamId: String, navController: NavHostController) {
    var objective by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var selectedExercises by remember { mutableStateOf<List<String>>(emptyList()) }
    var showExerciseDialog by remember { mutableStateOf(false) }
    var showNewExerciseDialog by remember { mutableStateOf(false) }
    var newExerciseName by remember { mutableStateOf("") }
    var newExerciseDescription by remember { mutableStateOf("") }
    var newExerciseDuration by remember { mutableStateOf("15") }

    val scope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Entrenamiento") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextField(
                value = objective,
                onValueChange = { objective = it },
                label = { Text("Objetivo") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            TextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Fecha (yyyy-mm-dd)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            TextField(
                value = duration,
                onValueChange = { duration = it },
                label = { Text("Duración (minutos)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Sección de ejercicios
            Text(
                text = "Ejercicios (${selectedExercises.size} seleccionados)",
                style = MaterialTheme.typography.titleSmall
            )

            Button(
                onClick = { showExerciseDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Seleccionar Ejercicios")
            }

            Button(
                onClick = { showNewExerciseDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Crear Nuevo Ejercicio")
            }

            // Mostrar ejercicios seleccionados
            if (selectedExercises.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedExercises.forEach { exerciseId ->
                            Text(
                                "• $exerciseId",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            if (error.isNotEmpty()) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    if (objective.isBlank()) {
                        error = "Objetivo requerido"
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        try {
                            // Parse date string to timestamp
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val parsedDate = try {
                                dateFormat.parse(date)?.time ?: System.currentTimeMillis()
                            } catch (e: Exception) {
                                System.currentTimeMillis()
                            }

                            val training = Training(
                                id = "",
                                teamId = teamId,
                                objective = objective,
                                date = parsedDate,
                                duration = duration.toIntOrNull() ?: 60,
                                exercises = selectedExercises,
                                createdAt = System.currentTimeMillis()
                            )
                            firestore.collection("teams").document(teamId)
                                .collection("trainings").add(training)
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
                Text(if (isLoading) "Guardando..." else "Crear Entrenamiento")
            }
        }

        // Diálogo para seleccionar ejercicios
        if (showExerciseDialog) {
            ExerciseSelectionDialog(
                onDismiss = { showExerciseDialog = false },
                onExercisesSelected = { exercises ->
                    selectedExercises = exercises
                    showExerciseDialog = false
                }
            )
        }

        // Diálogo para crear nuevo ejercicio
        if (showNewExerciseDialog) {
            CreateExerciseDialog(
                onDismiss = { showNewExerciseDialog = false },
                onExerciseCreated = { exercise ->
                    selectedExercises = selectedExercises + exercise.id
                    showNewExerciseDialog = false
                    newExerciseName = ""
                    newExerciseDescription = ""
                    newExerciseDuration = "15"
                }
            )
        }
    }
}

/**
 * Diálogo para seleccionar ejercicios de la biblioteca.
 */
@Composable
fun ExerciseSelectionDialog(
    onDismiss: () -> Unit,
    onExercisesSelected: (List<String>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val exercisesList = remember {
        pe.edu.esan.sportpro.data.model.ExerciseLibrary.defaultExercises
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Ejercicios") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(exercisesList) { exercise ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = exercise.id in selectedIds,
                            onCheckedChange = { isChecked ->
                                selectedIds = if (isChecked) {
                                    selectedIds + exercise.id
                                } else {
                                    selectedIds - exercise.id
                                }
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                exercise.name,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                exercise.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onExercisesSelected(selectedIds.toList()) }) {
                Text("Seleccionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Diálogo para crear un nuevo ejercicio.
 */
@Composable
fun CreateExerciseDialog(
    onDismiss: () -> Unit,
    onExerciseCreated: (Exercise) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("15") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear Nuevo Ejercicio") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
                TextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duración (min)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (error.isNotEmpty()) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = "Nombre requerido"
                        return@Button
                    }
                    val exercise = Exercise(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        description = description,
                        duration = duration.toIntOrNull() ?: 15,
                        difficulty = ExerciseDifficulty.MEDIO
                    )
                    onExerciseCreated(exercise)
                }
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
