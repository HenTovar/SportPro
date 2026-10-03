package pe.edu.esan.sportpro.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.*
import pe.edu.esan.sportpro.data.model.Team

class TeamRepositoryTest {
    @Test
    fun listUsesDocumentIdEvenWhenStoredIdIsEmptyOrWrong() = runTest {
        val db = mock<FirebaseFirestore>()
        val collection = mock<CollectionReference>()
        val query = mock<Query>()
        val snapshot = mock<QuerySnapshot>()
        val blank = mock<DocumentSnapshot>()
        val wrong = mock<DocumentSnapshot>()
        whenever(db.collection("teams")).thenReturn(collection)
        whenever(collection.get()).thenReturn(Tasks.forResult(snapshot))
        whenever(snapshot.documents).thenReturn(listOf(blank, wrong))
        whenever(blank.getBoolean("active")).thenReturn(true)
        whenever(wrong.getBoolean("active")).thenReturn(true)
        whenever(blank.id).thenReturn("actual-a")
        whenever(wrong.id).thenReturn("actual-b")
        whenever(blank.toObject(Team::class.java)).thenReturn(Team(id = "", name = "A"))
        whenever(wrong.toObject(Team::class.java)).thenReturn(Team(id = "stale", name = "B"))
        val result = TeamRepository(db).getAllTeams().toList().last() as Result.Success
        assertEquals(listOf("actual-a", "actual-b"), result.data!!.map { it.id })
    }

    @Test
    fun singleTeamUsesDocumentIdForLegacyDocument() = runTest {
        val db = mock<FirebaseFirestore>()
        val collection = mock<CollectionReference>()
        val reference = mock<DocumentReference>()
        val snapshot = mock<DocumentSnapshot>()
        whenever(db.collection("teams")).thenReturn(collection)
        whenever(collection.document("actual-a")).thenReturn(reference)
        whenever(reference.get()).thenReturn(Tasks.forResult(snapshot))
        whenever(snapshot.id).thenReturn("actual-a")
        whenever(snapshot.toObject(Team::class.java)).thenReturn(Team(id = "", name = "A"))
        val result = TeamRepository(db).getTeamById("actual-a").toList().last() as Result.Success
        assertEquals("actual-a", result.data!!.id)
    }
}
