package pe.edu.esan.sportpro.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.Team

/**
 * Repositorio para gestión de equipos.
 */
class TeamRepository(
    private val firestore: FirebaseFirestore
) {
    /**
     * Obtiene todos los equipos activos.
     */
    fun getAllTeams(): Flow<Result<List<Team>>> = flow {
        try {
            emit(Result.Loading())
            val snapshot = firestore.collection("teams")
                .whereEqualTo("isActive", true)
                .orderBy("name")
                .get()
                .await()

            val teams = snapshot.documents.mapNotNull { it.toObject(Team::class.java) }
            emit(Result.Success(teams))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Error al obtener equipos"))
        }
    }

    /**
     * Obtiene un equipo por su ID.
     */
    fun getTeamById(teamId: String): Flow<Result<Team>> = flow {
        try {
            emit(Result.Loading())
            val snapshot = firestore.collection("teams").document(teamId).get().await()
            val team = snapshot.toObject(Team::class.java) ?: throw Exception("Equipo no encontrado")
            emit(Result.Success(team))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Error al obtener equipo"))
        }
    }

    /**
     * Crea un nuevo equipo.
     */
    fun createTeam(team: Team): Flow<Result<Team>> = flow {
        try {
            emit(Result.Loading())
            val newTeamId = firestore.collection("teams").document().id
            val newTeam = team.copy(
                id = newTeamId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            firestore.collection("teams").document(newTeamId).set(newTeam).await()
            emit(Result.Success(newTeam))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Error al crear equipo"))
        }
    }

    /**
     * Actualiza un equipo existente.
     */
    fun updateTeam(team: Team): Flow<Result<Team>> = flow {
        try {
            emit(Result.Loading())
            val updatedTeam = team.copy(updatedAt = System.currentTimeMillis())
            firestore.collection("teams").document(team.id).set(updatedTeam).await()
            emit(Result.Success(updatedTeam))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Error al actualizar equipo"))
        }
    }

    /**
     * Elimina (desactiva) un equipo.
     */
    fun deleteTeam(teamId: String): Flow<Result<Boolean>> = flow {
        try {
            emit(Result.Loading())
            firestore.collection("teams").document(teamId).update("isActive", false).await()
            emit(Result.Success(true))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Error al eliminar equipo"))
        }
    }
}
