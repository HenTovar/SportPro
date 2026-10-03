package pe.edu.esan.sportpro.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.Exercise
import java.util.UUID

/**
 * Repositorio para gestión de la biblioteca de ejercicios.
 */
class ExerciseRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    /**
     * Obtiene todos los ejercicios de la biblioteca.
     */
    fun getAllExercises(): Flow<Result<List<Exercise>>> = flow {
        try {
            emit(Result.Loading())
            val snapshot = firestore.collection("exercises")
                .orderBy("name")
                .get()
                .await()

            val exercises = snapshot.documents.mapNotNull { it.toObject(Exercise::class.java) }
            emit(Result.Success(exercises))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Result.Error(e.message ?: "Error al obtener ejercicios"))
        }
    }

    /**
     * Crea un nuevo ejercicio en la biblioteca.
     */
    fun createExercise(exercise: Exercise): Flow<Result<Exercise>> = flow {
        try {
            emit(Result.Loading())
            val newExerciseId = UUID.randomUUID().toString()
            val newExercise = exercise.copy(id = newExerciseId)

            firestore.collection("exercises")
                .document(newExerciseId)
                .set(newExercise)
                .await()

            emit(Result.Success(newExercise))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Result.Error(e.message ?: "Error al crear ejercicio"))
        }
    }

    /**
     * Obtiene un ejercicio por su ID.
     */
    fun getExerciseById(exerciseId: String): Flow<Result<Exercise>> = flow {
        try {
            emit(Result.Loading())
            val snapshot = firestore.collection("exercises")
                .document(exerciseId)
                .get()
                .await()

            val exercise = snapshot.toObject(Exercise::class.java) ?: throw Exception("Ejercicio no encontrado")
            emit(Result.Success(exercise))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Result.Error(e.message ?: "Error al obtener ejercicio"))
        }
    }

    /**
     * Obtiene ejercicios por lista de IDs.
     */
    fun getExercisesByIds(exerciseIds: List<String>): Flow<Result<List<Exercise>>> = flow {
        try {
            emit(Result.Loading())
            if (exerciseIds.isEmpty()) {
                emit(Result.Success(emptyList()))
                return@flow
            }

            val exercises = mutableListOf<Exercise>()
            for (id in exerciseIds) {
                val snapshot = firestore.collection("exercises")
                    .document(id)
                    .get()
                    .await()
                val exercise = snapshot.toObject(Exercise::class.java)
                if (exercise != null) {
                    exercises.add(exercise)
                }
            }
            emit(Result.Success(exercises))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Result.Error(e.message ?: "Error al obtener ejercicios"))
        }
    }
}
