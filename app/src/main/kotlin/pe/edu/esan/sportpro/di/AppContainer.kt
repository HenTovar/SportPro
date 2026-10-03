package pe.edu.esan.sportpro.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import pe.edu.esan.sportpro.data.repository.AuthRepository
import pe.edu.esan.sportpro.data.repository.TeamRepository
import pe.edu.esan.sportpro.data.repository.PlayerRepository
import pe.edu.esan.sportpro.data.repository.TrainingRepository
import pe.edu.esan.sportpro.data.repository.ExerciseRepository

/**
 * Contenedor de dependencias de la aplicación.
 * Proporciona instancias únicas de repositorios y servicios.
 */
interface AppContainer {
    val authRepository: AuthRepository
    val teamRepository: TeamRepository
    val playerRepository: PlayerRepository
    val trainingRepository: TrainingRepository
    val exerciseRepository: ExerciseRepository
}

/**
 * Implementación del contenedor de dependencias.
 */
class DefaultAppContainer : AppContainer {
    // Firebase instances
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Repositories
    override val authRepository: AuthRepository by lazy {
        AuthRepository(firebaseAuth, firestore)
    }

    override val teamRepository: TeamRepository by lazy {
        TeamRepository(firestore)
    }

    override val playerRepository: PlayerRepository by lazy {
        PlayerRepository(firestore, storage)
    }

    override val trainingRepository: TrainingRepository by lazy {
        TrainingRepository(firestore)
    }

    override val exerciseRepository: ExerciseRepository by lazy {
        ExerciseRepository(firestore, storage)
    }
}
