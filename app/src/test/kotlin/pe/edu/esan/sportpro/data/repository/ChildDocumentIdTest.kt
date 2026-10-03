package pe.edu.esan.sportpro.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.*
import pe.edu.esan.sportpro.data.model.Player
import pe.edu.esan.sportpro.data.model.Training

class ChildDocumentIdTest {
    private fun collection(db: FirebaseFirestore, name: String): CollectionReference {
        val teams = mock<CollectionReference>()
        val team = mock<DocumentReference>()
        val children = mock<CollectionReference>()
        whenever(db.collection("teams")).thenReturn(teams)
        whenever(teams.document("team")).thenReturn(team)
        whenever(team.collection(name)).thenReturn(children)
        return children
    }

    @Test fun playersUseDocumentIdInsteadOfEmptyField() = runTest {
        val db = mock<FirebaseFirestore>()
        val children = collection(db, "players")
        val query = mock<Query>()
        val snapshot = mock<QuerySnapshot>()
        val doc = mock<DocumentSnapshot>()
        whenever(children.get()).thenReturn(Tasks.forResult(snapshot))
        whenever(snapshot.documents).thenReturn(listOf(doc))
        whenever(doc.getBoolean("active")).thenReturn(true)
        whenever(doc.id).thenReturn("player-document")
        whenever(doc.toObject(Player::class.java)).thenReturn(Player(id = ""))
        val result = PlayerRepository(db, mock<FirebaseStorage>()).getPlayersByTeam("team").toList().last() as Result.Success
        assertEquals("player-document", result.data.single().id)
    }

    @Test fun trainingsUseDocumentIdInsteadOfStaleField() = runTest {
        val db = mock<FirebaseFirestore>()
        val children = collection(db, "trainings")
        val query = mock<Query>()
        val snapshot = mock<QuerySnapshot>()
        val doc = mock<DocumentSnapshot>()
        whenever(children.orderBy("date", Query.Direction.DESCENDING)).thenReturn(query)
        whenever(query.get()).thenReturn(Tasks.forResult(snapshot))
        whenever(snapshot.documents).thenReturn(listOf(doc))
        whenever(doc.id).thenReturn("training-document")
        whenever(doc.toObject(Training::class.java)).thenReturn(Training(id = "stale"))
        val result = TrainingRepository(db).getTrainingsByTeam("team").toList().last() as Result.Success
        assertEquals("training-document", result.data.single().id)
    }
}
