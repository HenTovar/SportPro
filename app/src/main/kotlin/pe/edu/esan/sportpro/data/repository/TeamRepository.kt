package pe.edu.esan.sportpro.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import pe.edu.esan.sportpro.data.model.UserRole
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
            val actor = checkNotNull(currentProfile()) { "Inicia sesión" }
            check(actor.isActive && actor.role in setOf(UserRole.ADMIN, UserRole.ENTRENADOR)) { "Requiere personal activo" }
            require(team.name.trim().length in 1..120 && team.description.length <= 2000) { "Revisa el nombre y la descripción" }
            val randomId = firestore.collection("teams").document().id
            val newTeamId = if (actor.role == UserRole.ENTRENADOR) "coach_${actor.uid}_$randomId" else randomId
            val now = System.currentTimeMillis()
            val newTeam = team.copy(id=newTeamId, name=team.name.trim(), ownerId=actor.uid, createdAt=now, updatedAt=now, isActive=true)
            val reference = firestore.collection("teams").document(newTeamId)
            if (actor.role == UserRole.ADMIN) reference.set(newTeam).await()
            else firestore.runTransaction { transaction ->
                val accountRef = firestore.collection("users").document(actor.uid)
                val account = transaction.get(accountRef)
                check(account.getString("role") == "ENTRENADOR" && account.getBoolean("active") != false) { "Requiere entrenador activo" }
                val raw = account.get("coachTeamIds")
                check(raw is List<*>) { "Un administrador debe confirmar primero tus equipos en Perfil" }
                check(raw.all { it is String } && raw.size < 20) { "Límite de 20 equipos; consulta al administrador" }
                val ids = raw.filterIsInstance<String>()
                transaction.set(reference, mapOf("id" to newTeamId, "name" to newTeam.name,
                    "category" to newTeam.category.name, "description" to newTeam.description,
                    "ownerId" to actor.uid, "createdAt" to now, "updatedAt" to now,
                    "active" to true, "coachCreated" to true))
                transaction.update(accountRef, mapOf("coachTeamIds" to ids + newTeamId,
                    "lastCreatedTeamId" to newTeamId, "coachScopeBy" to actor.uid,
                    "coachScopeAt" to FieldValue.serverTimestamp()))
            }.await()
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
            // Preserve creation seals and future server-owned fields when editing metadata.
            firestore.collection("teams").document(team.id).update(mapOf(
                "name" to updatedTeam.name, "category" to updatedTeam.category.name,
                "description" to updatedTeam.description, "active" to updatedTeam.isActive,
                "updatedAt" to updatedTeam.updatedAt)).await()
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
