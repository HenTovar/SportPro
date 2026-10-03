package pe.edu.esan.sportpro.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
            require(role in pe.edu.esan.sportpro.data.model.RolePolicy.registrationRoles) { "El administrador se asigna fuera del registro" }
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: throw Exception("No UID obtenido")

            // Crear documento de usuario en Firestore
            val user = User(
                uid = uid,
                email = email,
                name = name,
                role = role,
                createdAt = System.currentTimeMillis()
            )

            firestore.collection("users").document(uid).set(user).await()
            emit(Result.Success(user))
        } catch (e: Exception) {
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
            val user = userDoc.toObject(User::class.java)?.copy(uid = userDoc.id) ?: throw Exception("Usuario no encontrado")

            emit(Result.Success(user))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Error desconocido"))
        }
    }

    /**
     * Obtiene el usuario actual autenticado.
     */
    fun getCurrentUser(): Flow<Result<User?>> = flow {
        try {
            emit(Result.Loading())
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                val user = userDoc.toObject(User::class.java)?.copy(uid = userDoc.id)
                emit(Result.Success(user))
            } else {
                emit(Result.Success(null))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Error desconocido"))
        }
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
