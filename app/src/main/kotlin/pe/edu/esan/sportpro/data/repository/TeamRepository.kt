package pe.edu.esan.sportpro.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.Team

/**
 * Repositorio para gestión de equipos.
 */
class TeamRepository(
    private val firestore: FirebaseFirestore,
    private val currentProfile: suspend () -> pe.edu.esan.sportpro.data.model.User? = {
        val result = AuthRepository(com.google.firebase.auth.FirebaseAuth.getInstance(), firestore).getCurrentUser()
            .first { it !is Result.Loading }
        (result as? Result.Success)?.data
    }
) {
    /**
     * Obtiene todos los equipos activos.
     */
    fun getAllTeams(): Flow<Result<List<Team>>> = flow {
        try {
            emit(Result.Loading())
            // Sin where+orderBy combinados: eso exige un índice compuesto en Firestore.
            // Firestore guarda el Boolean `isActive` de Kotlin como el campo "active".
            // El id sale del documento (equipos antiguos podían tener id="" guardado).
            val user = checkNotNull(currentProfile()) { "Inicia sesión" }
            check(user.isActive) { "Cuenta inactiva" }
            val docs = if (user.role == pe.edu.esan.sportpro.data.model.UserRole.ADMIN) {
                firestore.collection("teams").get().await().documents
            } else {
                val ids = if (user.role == pe.edu.esan.sportpro.data.model.UserRole.ENTRENADOR) user.coachTeamIds
                    else (pe.edu.esan.sportpro.data.model.FamilyPolicy.children(user).map { it.teamId } +
                        if (user.hasFamilyRevocation) "" else user.teamId)
                ids.filter { it.isNotBlank() }.distinct().map { firestore.collection("teams").document(it).get().await() }
            }
            val teams = docs
                .filter { it.getBoolean("active") != false }
                .mapNotNull { doc -> doc.toObject(Team::class.java)?.copy(id = doc.id) }
                .sortedBy { it.name.lowercase() }
            emit(Result.Success(teams))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
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
            val team = snapshot.toObject(Team::class.java)?.copy(id = snapshot.id)
                ?: throw Exception("Equipo no encontrado")
            emit(Result.Success(team))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
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
            if (e is CancellationException) throw e
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
            if (e is CancellationException) throw e
            emit(Result.Error(e.message ?: "Error al actualizar equipo"))
        }
    }

    /**
     * Elimina (desactiva) un equipo.
     */
    fun deleteTeam(teamId: String): Flow<Result<Boolean>> = flow {
        try {
            emit(Result.Loading())
            firestore.collection("teams").document(teamId).update("active", false).await()
            emit(Result.Success(true))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Result.Error(e.message ?: "Error al eliminar equipo"))
        }
    }
}
