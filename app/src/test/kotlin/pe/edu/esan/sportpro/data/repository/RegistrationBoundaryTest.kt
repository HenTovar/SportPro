package pe.edu.esan.sportpro.data.repository

import com.google.firebase.auth.*
import com.google.firebase.firestore.*
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import pe.edu.esan.sportpro.data.model.*

class RegistrationBoundaryTest {
    private val auth: FirebaseAuth = mock()
    private val db: FirebaseFirestore = mock()
    private val users: CollectionReference = mock()
    private val requests: CollectionReference = mock()
    private val profile: DocumentReference = mock()
    private val request: DocumentReference = mock()
    private val authResult: AuthResult = mock()
    private val identity: FirebaseUser = mock()
    private lateinit var repo: AuthRepository
    @Before fun setup() {
        whenever(identity.uid).thenReturn("applicant")
        whenever(identity.email).thenReturn("a@example.test")
        whenever(authResult.user).thenReturn(identity)
        whenever(auth.createUserWithEmailAndPassword(any(),any())).thenReturn(Tasks.forResult(authResult))
        whenever(db.collection("users")).thenReturn(users)
        whenever(db.collection("roleRequests")).thenReturn(requests)
        whenever(users.document("applicant")).thenReturn(profile)
        whenever(requests.document("applicant")).thenReturn(request)
        whenever(profile.set(any<Any>())).thenReturn(Tasks.forResult(null))
        whenever(request.set(any<Any>())).thenReturn(Tasks.forResult(null))
        repo=AuthRepository(auth,db)
    }
    @Test fun coachChoiceWritesBaseThenPendingRequestAndNeverStaff()=runTest {
        val rows=repo.register("a@example.test","secret12","Nombre",UserRole.ENTRENADOR).toList()
        val user=(rows.last() as Result.Success).data
        assertEquals(UserRole.JUGADOR,user.role)
        val profileArg=argumentCaptor<Any>(); val requestArg=argumentCaptor<Any>()
        val order=inOrder(profile,request)
        order.verify(profile).set(profileArg.capture()); order.verify(request).set(requestArg.capture())
        assertEquals(UserRole.JUGADOR,(profileArg.firstValue as User).role)
        val sent=requestArg.firstValue as Map<*,*>
        assertEquals("JUGADOR",sent["fromRole"]);assertEquals("ENTRENADOR",sent["toRole"]);assertEquals("PENDING",sent["status"])
        assertEquals("applicant",sent["uid"]);assertEquals("a@example.test",sent["email"])
        assertEquals(FieldValue.serverTimestamp(),sent["createdAt"])
        verify(profile,never()).update(any<String>(),any<Any>())
    }
    @Test fun failedRequestReportsPartialBaseAccountNotFalseSuccess()=runTest {
        whenever(request.set(any<Any>())).thenReturn(Tasks.forException(Exception("permission denied")))
        val rows=repo.register("a@example.test","secret12","Nombre",UserRole.ENTRENADOR).toList()
        assertFalse(rows.any{it is Result.Success})
        assertTrue((rows.last() as Result.Error).message.contains("cuenta se creó como Jugador"))
        val arg=argumentCaptor<Any>();verify(profile).set(arg.capture());assertEquals(UserRole.JUGADOR,(arg.firstValue as User).role)
    }
    @Test fun ordinaryParentSignupDoesNotRequestStaff()=runTest {
        val user=(repo.register("a@example.test","secret12","Nombre",UserRole.PADRE).toList().last() as Result.Success).data
        assertEquals(UserRole.PADRE,user.role);verify(request,never()).set(any<Any>())
    }
    @Test fun adminRejectedBeforeAuthAccountIsCreated()=runTest {
        val result=repo.register("a@example.test","secret12","Nombre",UserRole.ADMIN).toList().last()
        assertTrue(result is Result.Error);verify(auth,never()).createUserWithEmailAndPassword(any(),any())
    }
}
