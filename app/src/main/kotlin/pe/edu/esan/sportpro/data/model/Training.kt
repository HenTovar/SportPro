package pe.edu.esan.sportpro.data.model

/**
 * Modelo de datos para una sesión de entrenamiento.
 */
data class Training(
    val id: String = "",
    val teamId: String = "",
    val name: String = "",
    val date: Long = 0,
    val duration: Int = 90,
    val objective: String = "",
    val description: String = "",
    val exercises: List<String> = emptyList(),
    val status: TrainingStatus = TrainingStatus.PROGRAMADO,
    val createdBy: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

/**
 * Estados posibles de una sesión de entrenamiento.
 */
enum class TrainingStatus {
    PROGRAMADO,
    EN_CURSO,
    COMPLETADO,
    CANCELADO
}

/**
 * Modelo de ejercicio reutilizable en entrenamientos.
 */
data class Exercise(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val duration: Int = 15,
    val difficulty: ExerciseDifficulty = ExerciseDifficulty.MEDIO,
    val materials: List<String> = emptyList()
)

/**
 * Niveles de dificultad de ejercicios.
 */
enum class ExerciseDifficulty {
    BAJO,
    MEDIO,
    ALTO,
    AVANZADO
}

/**
 * Biblioteca de ejercicios predefinidos.
 */
object ExerciseLibrary {
    val defaultExercises = listOf(
        Exercise(
            id = "1",
            name = "Calentamiento",
            description = "Trote suave y estiramientos",
            duration = 10,
            difficulty = ExerciseDifficulty.BAJO
        ),
        Exercise(
            id = "2",
            name = "Rondas de pase",
            description = "Ejercicio de precisión de pase",
            duration = 15,
            difficulty = ExerciseDifficulty.MEDIO
        ),
        Exercise(
            id = "3",
            name = "Regateo",
            description = "Trabajo individual de regate",
            duration = 15,
            difficulty = ExerciseDifficulty.MEDIO
        ),
        Exercise(
            id = "4",
            name = "Partido táctico",
            description = "Partido completo con énfasis táctico",
            duration = 30,
            difficulty = ExerciseDifficulty.ALTO
        ),
        Exercise(
            id = "5",
            name = "Ejercicio defensivo",
            description = "Ejercicios de defensa y marca",
            duration = 15,
            difficulty = ExerciseDifficulty.MEDIO
        ),
        Exercise(
            id = "6",
            name = "Tiros a portería",
            description = "Ejercicio de remate",
            duration = 15,
            difficulty = ExerciseDifficulty.MEDIO
        )
    )
}
