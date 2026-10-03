package pe.edu.esan.sportpro.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*
import pe.edu.esan.sportpro.data.model.User

class CurrentUserFlowTest {
    @Test fun firstProfileDoesNotCatchCollectorsInternalCancellation() = runTest {
        val auth = mock<FirebaseAuth>()
        val db = mock<FirebaseFirestore>()
        val firebaseUser = mock<FirebaseUser>()
        val users = mock<CollectionReference>()
        val doc = mock<DocumentReference>()
        val snapshot = mock<DocumentSnapshot>()
        whenever(auth.currentUser).thenReturn(firebaseUser)
        whenever(firebaseUser.uid).thenReturn("henry")
        whenever(db.collection("users")).thenReturn(users)
        whenever(users.document("henry")).thenReturn(doc)
        whenever(doc.get()).thenReturn(Tasks.forResult(snapshot))
        whenever(snapshot.id).thenReturn("henry")
        whenever(snapshot.toObject(User::class.java)).thenReturn(User(name="Henry"))
        val result = AuthRepository(auth, db).getCurrentUser().first { it !is Result.Loading }
        assertEquals("henry", (result as Result.Success).data?.uid)
    }

    @Test fun malformedProfileIsReturnedAsError() = runTest {
        val auth = mock<FirebaseAuth>()
        val db = mock<FirebaseFirestore>()
        val firebaseUser = mock<FirebaseUser>()
        val users = mock<CollectionReference>()
        val doc = mock<DocumentReference>()
        val snapshot = mock<DocumentSnapshot>()
        whenever(auth.currentUser).thenReturn(firebaseUser)
        whenever(firebaseUser.uid).thenReturn("henry")
        whenever(db.collection("users")).thenReturn(users)
        whenever(users.document("henry")).thenReturn(doc)
        whenever(doc.get()).thenReturn(Tasks.forResult(snapshot))
        whenever(snapshot.id).thenReturn("henry")
        whenever(snapshot.toObject(User::class.java)).thenThrow(IllegalArgumentException("Unknown role"))
        val result = AuthRepository(auth, db).getCurrentUser().first { it !is Result.Loading }
        assertEquals("Unknown role", (result as Result.Error).message)
    }

    @Test fun firstWithoutSessionDoesNotEmptyFlowWithTransparencyError() = runTest {
        val auth = mock<FirebaseAuth>()
        val result = AuthRepository(auth, mock()).getCurrentUser().first { it !is Result.Loading }
        assertTrue(result is Result.Success)
        assertNull((result as Result.Success).data)
    }
}
