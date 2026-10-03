package pe.edu.esan.sportpro.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.Training
import pe.edu.esan.sportpro.data.model.AttendanceRecord

/**
 * Repositorio para gestión de entrenamientos.
 */
class TrainingRepository(
    private val firestore: FirebaseFirestore
) {
    /**
     * Obtiene todos los entrenamientos de un equipo.
     */
    fun getTrainingsByTeam(teamId: String): Flow<Result<List<Training>>> = flow {
        try {
            emit(Result.Loading())
            val snapshot = firestore.collection("teams")
                .document(teamId)
                .collection("trainings")
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val trainings = snapshot.documents.mapNotNull { it.toObject(Training::class.java) }
            emit(Result.Success(trainings))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Error al obtener entrenamientos"))
        }
    }

    /**
     * Crea un nuevo entrenamiento.
     */
    fun createTraining(teamId: String, training: Training): Flow<Result<Training>> = flow {
        try {
            emit(Result.Loading())
            val newTrainingId = firestore.collection("teams")
                .document(teamId)
                .collection("trainings")
                .document()
                .id

            val newTraining = training.copy(
                id = newTrainingId,
                teamId = teamId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            firestore.collection("teams")
                .document(teamId)
                .collection("trainings")
                .document(newTrainingId)
                .set(newTraining)
                .await()

            emit(Result.Success(newTraining))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Error al crear entrenamiento"))
        }
    }

    /**
     * Actualiza un entrenamiento existente.
     */
    fun updateTraining(teamId: String, training: Training): Flow<Result<Training>> = flow {
        try {
            emit(Result.Loading())
            val updatedTraining = training.copy(updatedAt = System.currentTimeMillis())

            firestore.collection("teams")
                .document(teamId)
                .collection("trainings")
                .document(training.id)
                .set(updatedTraining)
                .await()

            emit(Result.Success(updatedTraining))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Error al actualizar entrenamiento"))
        }
    }

    /**
     * Registra la asistencia de un jugador a un entrenamiento.
     */
    fun recordAttendance(teamId: String, attendance: AttendanceRecord): Flow<Result<AttendanceRecord>> = flow {
        try {
            emit(Result.Loading())
            val newAttendanceId = firestore.collection("teams")
                .document(teamId)
                .collection("attendance")
                .document()
                .id

            val newAttendance = attendance.copy(
                id = newAttendanceId,
                recordedAt = System.currentTimeMillis()
            )

            firestore.collection("teams")
                .document(teamId)
                .collection("attendance")
                .document(newAttendanceId)
                .set(newAttendance)
                .await()

            emit(Result.Success(newAttendance))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Error al registrar asistencia"))
        }
    }

    /**
     * Obtiene el historial de asistencia de un jugador.
     */
    fun getAttendanceHistory(teamId: String, playerId: String): Flow<Result<List<AttendanceRecord>>> = flow {
        try {
            emit(Result.Loading())
            val snapshot = firestore.collection("teams")
                .document(teamId)
                .collection("attendance")
                .whereEqualTo("playerId", playerId)
                .get()
                .await()

            // Orden en la app: where+orderBy en campos distintos exige índice compuesto.
            val records = snapshot.documents
                .mapNotNull { it.toObject(AttendanceRecord::class.java) }
                .sortedByDescending { it.recordedAt }
            emit(Result.Success(records))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Error al obtener historial de asistencia"))
        }
    }
}
