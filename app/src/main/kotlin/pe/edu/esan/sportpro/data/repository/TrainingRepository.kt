package pe.edu.esan.sportpro.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
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

            val trainings = snapshot.documents.mapNotNull { doc -> doc.toObject(Training::class.java)?.copy(id = doc.id) }
            emit(Result.Success(trainings))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
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
            if (e is CancellationException) throw e
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
            if (e is CancellationException) throw e
            emit(Result.Error(e.message ?: "Error al actualizar entrenamiento"))
        }
    }

    /**
     * Registra la asistencia de un jugador a un entrenamiento.
     */
    fun recordAttendance(teamId: String, attendance: AttendanceRecord): Flow<Result<AttendanceRecord>> = flow {
        try {
            emit(Result.Loading())
            require(attendance.trainingId.isNotBlank() && attendance.playerId.isNotBlank()) {
                "Selecciona un entrenamiento y un jugador"
            }
            val collection = firestore.collection("teams").document(teamId).collection("attendance")
            // Reuse legacy random IDs; new pairs receive a stable ID, including concurrent saves.
            val existingId = attendance.id.ifBlank {
                collection.whereEqualTo("trainingId", attendance.trainingId).get().await()
                    .documents.mapNotNull { doc ->
                        doc.toObject(AttendanceRecord::class.java)?.copy(id = doc.id)
                    }.filter { it.playerId == attendance.playerId }
                    .maxByOrNull { it.recordedAt }?.id.orEmpty()
            }
            val newAttendanceId = existingId.ifBlank {
                java.util.UUID.nameUUIDFromBytes(
                    "${attendance.trainingId}/${attendance.playerId}".toByteArray(Charsets.UTF_8)
                ).toString()
            }
            val newAttendance = attendance.copy(
                id = newAttendanceId,
                recordedAt = System.currentTimeMillis()
            )
            collection.document(newAttendanceId).set(newAttendance).await()

            emit(Result.Success(newAttendance))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Result.Error(e.message ?: "Error al registrar asistencia"))
        }
    }

    /** Recovers saved marks before editing a session. */
    fun getAttendanceForTraining(teamId: String, trainingId: String): Flow<Result<List<AttendanceRecord>>> = flow {
        try {
            emit(Result.Loading())
            val snapshot = firestore.collection("teams").document(teamId).collection("attendance")
                .whereEqualTo("trainingId", trainingId).get().await()
            val records = snapshot.documents.mapNotNull { doc ->
                doc.toObject(AttendanceRecord::class.java)?.copy(id = doc.id)
            }.sortedByDescending { it.recordedAt }.distinctBy { it.playerId }
            emit(Result.Success(records))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Result.Error(e.message ?: "Error al recuperar asistencia"))
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
                .mapNotNull { doc -> doc.toObject(AttendanceRecord::class.java)?.copy(id = doc.id) }
                .sortedByDescending { it.recordedAt }
                .distinctBy { it.trainingId }
            emit(Result.Success(records))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Result.Error(e.message ?: "Error al obtener historial de asistencia"))
        }
    }
}
