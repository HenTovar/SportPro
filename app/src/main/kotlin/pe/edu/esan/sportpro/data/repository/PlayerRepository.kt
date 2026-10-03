package pe.edu.esan.sportpro.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.Player
import android.net.Uri

/**
 * Repositorio para gestión de jugadores.
 */
class PlayerRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    /**
     * Obtiene todos los jugadores de un equipo.
     */
    fun getPlayersByTeam(teamId: String): Flow<Result<List<Player>>> = flow {
        try {
            emit(Result.Loading())
            val snapshot = firestore.collection("teams")
                .document(teamId)
                .collection("players")
                .whereEqualTo("isActive", true)
                .orderBy("name")
                .get()
                .await()

            val players = snapshot.documents.mapNotNull { it.toObject(Player::class.java) }
            emit(Result.Success(players))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Error al obtener jugadores"))
        }
    }

    /**
     * Obtiene un jugador por su ID.
     */
    fun getPlayerById(teamId: String, playerId: String): Flow<Result<Player>> = flow {
        try {
            emit(Result.Loading())
            val snapshot = firestore.collection("teams")
                .document(teamId)
                .collection("players")
                .document(playerId)
                .get()
                .await()

            val player = snapshot.toObject(Player::class.java) ?: throw Exception("Jugador no encontrado")
            emit(Result.Success(player))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Error al obtener jugador"))
        }
    }

    /**
     * Crea un nuevo jugador en un equipo.
     */
    fun createPlayer(teamId: String, player: Player): Flow<Result<Player>> = flow {
        try {
            emit(Result.Loading())
            val newPlayerId = firestore.collection("teams").document(teamId)
                .collection("players").document().id

            val newPlayer = player.copy(
                id = newPlayerId,
                teamId = teamId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            firestore.collection("teams")
                .document(teamId)
                .collection("players")
                .document(newPlayerId)
                .set(newPlayer)
                .await()

            emit(Result.Success(newPlayer))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Error al crear jugador"))
        }
    }

    /**
     * Actualiza un jugador existente.
     */
    fun updatePlayer(teamId: String, player: Player): Flow<Result<Player>> = flow {
        try {
            emit(Result.Loading())
            val updatedPlayer = player.copy(updatedAt = System.currentTimeMillis())

            firestore.collection("teams")
                .document(teamId)
                .collection("players")
                .document(player.id)
                .set(updatedPlayer)
                .await()

            emit(Result.Success(updatedPlayer))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Error al actualizar jugador"))
        }
    }

    /**
     * Elimina (desactiva) un jugador.
     */
    fun deletePlayer(teamId: String, playerId: String): Flow<Result<Boolean>> = flow {
        try {
            emit(Result.Loading())
            firestore.collection("teams")
                .document(teamId)
                .collection("players")
                .document(playerId)
                .update("isActive", false)
                .await()

            emit(Result.Success(true))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Error al eliminar jugador"))
        }
    }

    /**
     * Carga una foto de jugador a Cloud Storage y retorna la URL.
     */
    fun uploadPlayerPhoto(playerId: String, photoUri: Uri): Flow<Result<String>> = flow {
        try {
            emit(Result.Loading())
            val photoRef = storage.reference.child("players/$playerId.jpg")
            photoRef.putFile(photoUri).await()
            val downloadUrl = photoRef.downloadUrl.await().toString()
            emit(Result.Success(downloadUrl))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Error al cargar foto"))
        }
    }
}
