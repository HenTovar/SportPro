package pe.edu.esan.sportpro.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.*

/** Runs the real repository callback; the SDK rule contract is tested separately. */
class GuidedJoinTransactionTest {
    @Test fun joinCreatesOwnPlayerWithoutReadingItBeforeMembershipExists() = runTest {
        val auth = mock<FirebaseAuth>()
        val actor = mock<FirebaseUser>()
        whenever(auth.currentUser).thenReturn(actor)
        whenever(actor.uid).thenReturn("u1")
        val db = mock<FirebaseFirestore>()
        val tx = mock<Transaction>()
        val code = "TEAM22222222"
        val refs = mutableMapOf<String, DocumentReference>()
        fun collection(path: String): CollectionReference {
            val c = mock<CollectionReference>()
            whenever(c.document(any())).thenAnswer { invocation ->
                val full = path + "/" + invocation.getArgument<String>(0)
                refs.getOrPut(full) { mock() }
            }
            return c
        }
        val users = collection("users")
        val codes = collection("teamCodes")
        val profiles = collection("guidedProfiles")
        val teams = collection("teams")
        whenever(db.collection("users")).thenReturn(users)
        whenever(db.collection("teamCodes")).thenReturn(codes)
        whenever(db.collection("guidedProfiles")).thenReturn(profiles)
        whenever(db.collection("teams")).thenReturn(teams)
        val user = users.document("u1")
        val gift = codes.document(code)
        val profile = profiles.document("u1")
        val team = teams.document("a")
        val claims = collection("teamCodes/$code/claims")
        val players = collection("teams/a/players")
        whenever(gift.collection("claims")).thenReturn(claims)
        whenever(team.collection("players")).thenReturn(players)
        val claim = claims.document("u1")
        val player = players.document("u1_$code")
        val u = mock<DocumentSnapshot>()
        val g = mock<DocumentSnapshot>()
        val p = mock<DocumentSnapshot>()
        val absent = mock<DocumentSnapshot>()
        whenever(tx.get(user)).thenReturn(u)
        whenever(tx.get(gift)).thenReturn(g)
        whenever(tx.get(profile)).thenReturn(p)
        whenever(tx.get(claim)).thenReturn(absent)
        // Old code must fail this regression: membership ACL denies this GET.
        whenever(tx.get(player)).thenThrow(IllegalStateException("player pre-read denied"))
        whenever(u.getString("role")).thenReturn("JUGADOR")
        whenever(u.getString("teamId")).thenReturn("")
        whenever(g.exists()).thenReturn(true)
        whenever(g.getString("teamId")).thenReturn("a")
        whenever(g.getLong("count")).thenReturn(0L)
        whenever(p.exists()).thenReturn(true)
        whenever(p.getString("name")).thenReturn("Adulto ficticio")
        whenever(p.getString("position")).thenReturn("DELANTERO")
        whenever(p.getString("dominantFoot")).thenReturn("RIGHT")
        whenever(absent.exists()).thenReturn(false)
        whenever(tx.update(any<DocumentReference>(), any<Map<String, Any?>>())).thenReturn(tx)
        whenever(tx.set(any<DocumentReference>(), any<Map<String, Any?>>())).thenReturn(tx)
        whenever(db.runTransaction(any<Transaction.Function<Any?>>())).thenAnswer { invocation ->
            val callback = invocation.getArgument<Transaction.Function<Any?>>(0)
            Tasks.forResult(callback.apply(tx))
        }

        GuidedAccountRepository(auth, db).joinTeam(code)

        verify(tx, never()).get(player)
        verify(tx).get(claim)
        verify(tx).update(eq(gift), eq(mapOf("count" to 1L)))
        val membership = argumentCaptor<Map<String, Any?>>()
        verify(tx).update(eq(user), membership.capture())
        assertEquals("a", membership.firstValue["teamId"])
        assertEquals("u1_$code", membership.firstValue["playerId"])
        val created = argumentCaptor<Map<String, Any?>>()
        verify(tx).set(eq(player), created.capture())
        assertEquals("u1", created.firstValue["userUid"])
        assertEquals(code, created.firstValue["joinCode"])
        verify(tx).set(eq(claim), any<Map<String, Any?>>())
    }
}
