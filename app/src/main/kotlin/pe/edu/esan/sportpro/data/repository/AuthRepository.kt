package pe.edu.esan.sportpro.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.User
import pe.edu.esan.sportpro.data.model.UserRole

/**
 * Repositorio para autenticación y gestión de usuarios.
 * Maneja registro, inicio de sesión y operaciones de usuarios en Firestore.
 */
class AuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private suspend fun hydrate(doc: com.google.firebase.firestore.DocumentSnapshot): User? {
        val base = doc.toObject(User::class.java)?.copy(uid = doc.id) ?: return null
        val coachIds = if (base.role != UserRole.ENTRENADOR) emptyList() else if (doc.contains("coachTeamIds")) {
            (doc.get("coachTeamIds") as? List<*>)?.filterIsInstance<String>()?.filter { it.isNotBlank() }?.distinct().orEmpty()
        } else {
            // Read only the legacy owner's filtered query, never all teams.
            val owned = firestore.collection("teams").whereEqualTo("ownerId", doc.id).get().await().documents.map { it.id }
            (owned + base.teamId).filter { it.isNotBlank() }.distinct()
        }
        val children = pe.edu.esan.sportpro.data.model.FamilyPolicy.decode(doc.get("children")).map { child ->
            if (child.name != "Ficha vinculada" || base.role != UserRole.PADRE) child else {
                try {
                    val player = firestore.collection("teams").document(child.teamId).collection("players").document(child.playerId).get().await()
                    child.copy(name = player.getString("name") ?: "Ficha vinculada")
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { child.copy(name = "Ficha no disponible") }
            }
        }
        return base.copy(children = children, coachTeamIds = coachIds,
            hasFamilyRevocation = doc.get("lastFamilyRevocation") != null)
    }

    /**
     * Registra un nuevo usuario con email, contraseña y rol.
     */
    fun register(
        email: String,
        password: String,
        name: String,
        role: UserRole
    ): Flow<Result<User>> = flow {
        try {
            emit(Result.Loading())
            val baseRole = pe.edu.esan.sportpro.data.model.RolePolicy.registrationBaseRole(role)
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: throw Exception("No UID obtenido")

            // Crear documento de usuario en Firestore
            val user = User(
                uid = uid,
                email = email,
                name = name,
                role = baseRole,
                createdAt = System.currentTimeMillis()
            )

            firestore.collection("users").document(uid).set(user).await()
            if (role == UserRole.ENTRENADOR) {
                try {
                    // The base profile must exist before validRequest can inspect its source role.
                    firestore.collection("roleRequests").document(uid).set(mapOf(
                        "uid" to uid, "email" to checkNotNull(authResult.user?.email),
                        "fromRole" to baseRole.name, "toRole" to UserRole.ENTRENADOR.name,
                        "status" to "PENDING", "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )).await()
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) {
                    throw IllegalStateException("Tu cuenta se creó como Jugador, pero no se pudo enviar la solicitud. Inicia sesión y solicítala desde Perfil; no tienes acceso de entrenador.", e)
                }
            }
            emit(Result.Success(user))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Result.Error(e.message ?: "Error desconocido"))
        }
    }

    /**
     * Inicia sesión con email y contraseña.
     */
    fun login(email: String, password: String): Flow<Result<User>> = flow {
        try {
            emit(Result.Loading())
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: throw Exception("No UID obtenido")

            // Obtener datos del usuario desde Firestore
            val userDoc = firestore.collection("users").document(uid).get().await()
            val user = hydrate(userDoc) ?: throw Exception("Usuario no encontrado")

            emit(Result.Success(user))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Result.Error(e.message ?: "Error desconocido"))
        }
    }

    /**
     * Obtiene el usuario actual autenticado.
     */
    fun getCurrentUser(): Flow<Result<User?>> = flow<Result<User?>> {
        emit(Result.Loading())
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
            val user = hydrate(userDoc)
            emit(Result.Success(user))
        } else {
            emit(Result.Success(null))
        }
    }.catch { e ->
        // Handle source errors, never cancellation from first()/a screen leaving.
        emit(Result.Error(e.message ?: "Error desconocido"))
    }

    /**
     * Cierra la sesión del usuario actual.
     */
    fun logout() {
        firebaseAuth.signOut()
    }

    /**
     * Valida la contraseña (mínimo 6 caracteres).
     */
    fun isPasswordValid(password: String): Boolean {
        return password.length >= 6
    }

    /**
     * Valida el email con expresión regular.
     */
    fun isEmailValid(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$"
        return email.matches(emailRegex.toRegex())
    }
}

/**
 * Resultado genérico para operaciones asincrónicas.
 */
sealed class Result<out T> {
    class Loading : Result<Nothing>()
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}
