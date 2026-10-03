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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException

import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.Exercise
import pe.edu.esan.sportpro.data.model.ExerciseDifficulty
import pe.edu.esan.sportpro.data.model.Training
import java.text.SimpleDateFormat
import java.text.ParsePosition
import java.util.*

@Composable
fun CreateTrainingScreen(teamId: String, navController: NavHostController, trainingId: String? = null) {
    var objective by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("16:00") }
    var duration by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var selectedExercises by remember { mutableStateOf<List<String>>(emptyList()) }
    var showExerciseDialog by remember { mutableStateOf(false) }
    var showNewExerciseDialog by remember { mutableStateOf(false) }
    var newExerciseName by remember { mutableStateOf("") }
    var newExerciseDescription by remember { mutableStateOf("") }
    var newExerciseDuration by remember { mutableStateOf("15") }

    var formReady by remember(trainingId) { mutableStateOf(false) }
    var checkingAccess by remember(trainingId) { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(teamId, trainingId) {
        try {
            requireStaff(firestore)
            if (trainingId != null) {
                val document = firestore.collection("teams").document(teamId)
                    .collection("trainings").document(trainingId).get().await()
                check(document.exists()) { "El entrenamiento ya no existe" }
                val training = checkNotNull(document.toObject(Training::class.java))
                objective = training.objective
                date = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(training.date))
                time = SimpleDateFormat("HH:mm", Locale.ROOT).format(Date(training.date))
                duration = training.duration.toString()
                selectedExercises = training.exercises
            }
            formReady = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar el entrenamiento"
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
                title = { Text(if (trainingId == null) "Crear Entrenamiento" else "Editar Entrenamiento") },
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
                value = time,
                onValueChange = { time = it },
                label = { Text("Hora (HH:mm, 24 horas)") },
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
                            val uid = requireStaff(firestore)
                            val parsedDate = parseTrainingDate(date, time)
                            val parsedDuration = duration.toIntOrNull()
                            require(parsedDuration != null && parsedDuration > 0) { "Indica una duración mayor que cero" }
                            val collection = firestore.collection("teams").document(teamId).collection("trainings")
                            val document = trainingId?.let(collection::document) ?: collection.document()
                            if (trainingId == null) {
                                document.set(Training(
                                    id = document.id,
                                    teamId = teamId,
                                    objective = objective.trim(),
                                    date = parsedDate,
                                    duration = parsedDuration,
                                    exercises = selectedExercises,
                                    createdBy = uid,
                                    createdAt = System.currentTimeMillis()
                                )).await()
                            } else {
                                document.update(mapOf(
                                    "id" to document.id,
                                    "objective" to objective.trim(),
                                    "date" to parsedDate,
                                    "duration" to parsedDuration,
                                    "exercises" to selectedExercises,
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
                Text(if (isLoading) "Guardando..." else if (trainingId == null) "Crear Entrenamiento" else "Guardar cambios")
            }
        }

        // Diálogo para seleccionar ejercicios
        if (showExerciseDialog) {
            ExerciseSelectionDialog(
                initialSelectedIds = selectedExercises,
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
    onExercisesSelected: (List<String>) -> Unit,
    initialSelectedIds: List<String> = emptyList()
) {
    var selectedIds by remember { mutableStateOf(initialSelectedIds.toSet()) }
    var exercisesList by remember {
        mutableStateOf(pe.edu.esan.sportpro.data.model.ExerciseLibrary.defaultExercises)
    }
    var loadError by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        try {
            val documents = FirebaseFirestore.getInstance().collection("exercises").get().await()
            val savedExercises = documents.documents.mapNotNull { document ->
                document.toObject(Exercise::class.java)?.copy(id = document.id)
            }
            exercisesList = (exercisesList + savedExercises).distinctBy { it.id }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            loadError = "No se pudo cargar la biblioteca completa. Se conservan los ejercicios seleccionados."
        }
    }
    val displayedExercises = exercisesList + initialSelectedIds.filter { id ->
        exercisesList.none { it.id == id }
    }.map { id -> Exercise(id = id, name = "Ejercicio guardado ($id)") }

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
                if (loadError.isNotEmpty()) {
                    item { Text(loadError, color = MaterialTheme.colorScheme.error) }
                }
                items(displayedExercises, key = { it.id }) { exercise ->
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
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                enabled = !saving,
                onClick = {
                    if (name.isBlank()) {
                        error = "Nombre requerido"
                        return@Button
                    }
                    val minutes = duration.toIntOrNull()
                    if (minutes == null || minutes <= 0) {
                        error = "Duración inválida"
                        return@Button
                    }
                    saving = true
                    scope.launch {
                        try {
                            val firestore = FirebaseFirestore.getInstance()
                            requireStaff(firestore)
                            val document = firestore.collection("exercises").document()
                            val exercise = Exercise(
                                id = document.id, name = name.trim(), description = description.trim(),
                                duration = minutes, difficulty = ExerciseDifficulty.MEDIO
                            )
                            document.set(exercise).await()
                            onExerciseCreated(exercise)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            error = e.message ?: "No se pudo guardar el ejercicio"
                        } finally {
                            saving = false
                        }
                    }
                }
            ) {
                Text(if (saving) "Guardando..." else "Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
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

private fun parseTrainingDate(date: String, time: String): Long {
    require(Regex("\\d{4}-\\d{2}-\\d{2}").matches(date) && Regex("\\d{2}:\\d{2}").matches(time)) {
        "Indica fecha yyyy-mm-dd y hora HH:mm válidas"
    }
    val value = "$date $time"
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).apply { isLenient = false }
    val cursor = ParsePosition(0)
    val parsed = formatter.parse(value, cursor)
    require(parsed != null && cursor.index == value.length) { "Fecha u hora inválida" }
    return parsed.time
}
