package pe.edu.esan.sportpro.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import pe.edu.esan.sportpro.data.model.Exercise
import pe.edu.esan.sportpro.data.model.ExerciseDifficulty
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

/**
 * Pruebas unitarias para ExerciseRepository.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseRepositoryTest {

    @Mock
    private lateinit var mockFirestore: FirebaseFirestore

    @Mock
    private lateinit var mockStorage: FirebaseStorage

    private lateinit var repository: ExerciseRepository

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        repository = ExerciseRepository(mockFirestore, mockStorage)
    }

    @Test
    fun testExerciseCreation() {
        val exercise = Exercise(
            id = "",
            name = "Calentamiento",
            description = "Trote suave",
            duration = 10,
            difficulty = ExerciseDifficulty.BAJO
        )

        assertThat(exercise.name).isEqualTo("Calentamiento")
        assertThat(exercise.duration).isEqualTo(10)
        assertThat(exercise.difficulty).isEqualTo(ExerciseDifficulty.BAJO)
    }

    @Test
    fun testExerciseWithMaterials() {
        val exercise = Exercise(
            id = "ex1",
            name = "Rondas de pase",
            description = "Ejercicio de precisión",
            duration = 15,
            difficulty = ExerciseDifficulty.MEDIO,
            materials = listOf("balón", "conos")
        )

        assertThat(exercise.materials).hasSize(2)
        assertThat(exercise.materials).contains("balón")
    }

    @Test
    fun testDefaultExerciseLibrary() {
        val defaultExercises = pe.edu.esan.sportpro.data.model.ExerciseLibrary.defaultExercises
        assertThat(defaultExercises).isNotEmpty()
        assertThat(defaultExercises).hasSize(6)
    }

    @Test
    fun testDefaultExerciseNames() {
        val defaultExercises = pe.edu.esan.sportpro.data.model.ExerciseLibrary.defaultExercises
        val names = defaultExercises.map { it.name }

        assertThat(names).contains("Calentamiento")
        assertThat(names).contains("Rondas de pase")
        assertThat(names).contains("Regateo")
        assertThat(names).contains("Partido táctico")
    }

    @Test
    fun testExerciseDifficultiesExist() {
        assertThat(ExerciseDifficulty.BAJO).isNotNull()
        assertThat(ExerciseDifficulty.MEDIO).isNotNull()
        assertThat(ExerciseDifficulty.ALTO).isNotNull()
        assertThat(ExerciseDifficulty.AVANZADO).isNotNull()
    }

    @Test
    fun testExerciseDefaultDuration() {
        val exercise = Exercise(
            id = "ex1",
            name = "Test",
            description = "Test exercise"
        )
        assertThat(exercise.duration).isEqualTo(15)
    }

    @Test
    fun testExerciseDefaultDifficulty() {
        val exercise = Exercise(
            id = "ex1",
            name = "Test",
            description = "Test exercise"
        )
        assertThat(exercise.difficulty).isEqualTo(ExerciseDifficulty.MEDIO)
    }

    @Test
    fun testMultipleExerciseCreation() {
        val exercises = listOf(
            Exercise(id = "1", name = "Ejercicio 1", duration = 10),
            Exercise(id = "2", name = "Ejercicio 2", duration = 15),
            Exercise(id = "3", name = "Ejercicio 3", duration = 20)
        )

        assertThat(exercises).hasSize(3)
        assertThat(exercises.map { it.duration }).containsExactly(10, 15, 20)
    }

    @Test
    fun testExerciseIds() {
        val exercise = Exercise(
            id = "unique-id-123",
            name = "Test",
            description = "Test"
        )

        assertThat(exercise.id).isEqualTo("unique-id-123")
    }

    @Test
    fun testPartidoTacticoExercise() {
        val defaultExercises = pe.edu.esan.sportpro.data.model.ExerciseLibrary.defaultExercises
        val partidoTactico = defaultExercises.find { it.name == "Partido táctico" }

        assertThat(partidoTactico).isNotNull()
        assertThat(partidoTactico?.duration).isEqualTo(30)
        assertThat(partidoTactico?.difficulty).isEqualTo(ExerciseDifficulty.ALTO)
    }
}
