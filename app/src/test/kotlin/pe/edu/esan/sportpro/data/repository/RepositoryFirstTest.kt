package pe.edu.esan.sportpro.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.*

class RepositoryFirstTest {
    private val db = mock<FirebaseFirestore>()
    private val collection = mock<CollectionReference>()
    private val document = mock<DocumentReference>()
    private val query = mock<Query>()
    private val snapshot = mock<QuerySnapshot>()

    private fun setup() {
        whenever(db.collection(any())).thenReturn(collection)
        whenever(collection.document(any())).thenReturn(document)
        whenever(document.collection(any())).thenReturn(collection)
        whenever(collection.orderBy(any<String>())).thenReturn(query)
        whenever(collection.orderBy(any<String>(), any<Query.Direction>())).thenReturn(query)
        whenever(collection.get()).thenReturn(Tasks.forResult(snapshot))
        whenever(query.get()).thenReturn(Tasks.forResult(snapshot))
        whenever(snapshot.documents).thenReturn(emptyList())
    }

    @Test fun teamsFirstSuccessDoesNotCrash() = runTest {
        setup()
        assertTrue(TeamRepository(db).getAllTeams().first { it !is Result.Loading } is Result.Success)
    }
    @Test fun playersFirstSuccessDoesNotCrash() = runTest {
        setup()
        assertTrue(PlayerRepository(db, mock()).getPlayersByTeam("team").first { it !is Result.Loading } is Result.Success)
    }
    @Test fun trainingsFirstSuccessDoesNotCrash() = runTest {
        setup()
        assertTrue(TrainingRepository(db).getTrainingsByTeam("team").first { it !is Result.Loading } is Result.Success)
    }
    @Test fun exercisesFirstSuccessDoesNotCrash() = runTest {
        setup()
        assertTrue(ExerciseRepository(db, mock()).getAllExercises().first { it !is Result.Loading } is Result.Success)
    }
}
