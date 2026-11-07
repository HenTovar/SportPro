package pe.edu.esan.sportpro.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.FamilyPolicy
import java.util.Date

/** The rules, not client clocks or UI checks, authorize issuance and atomic redemption. */
class FamilyRepository(private val db: FirebaseFirestore, private val auth: FirebaseAuth) {
    suspend fun issue(teamId: String, playerId: String): String {
        checkNotNull(auth.currentUser) { "Inicia sesión" }
        val player = db.collection("teams").document(teamId).collection("players").document(playerId).get().await()
        check(player.exists()) { "El jugador no existe" }
        val code = FamilyPolicy.newCode()
        val ref = db.collection("familyCodes").document(code)
        db.runTransaction { tx ->
            check(!tx.get(ref).exists()) { "Vuelve a generar el código" }
            tx.set(ref, mapOf("teamId" to teamId, "playerId" to playerId,
                "name" to player.getString("name").orEmpty(), "issuedBy" to auth.currentUser!!.uid,
                "createdAt" to FieldValue.serverTimestamp(), "expiresAt" to Timestamp(Date(System.currentTimeMillis() + 20 * 60_000)),
                "usedBy" to "", "usedAt" to null))
        }.await()
        return code
    }
    suspend fun redeem(input: String) {
        val uid = checkNotNull(auth.currentUser?.uid) { "Inicia sesión" }
        val code = FamilyPolicy.normalizeCode(input)
        require(FamilyPolicy.validCode(code)) { "El código debe tener 12 letras o números" }
        val codeRef = db.collection("familyCodes").document(code)
        val userRef = db.collection("users").document(uid)
        db.runTransaction { tx ->
            val account = tx.get(userRef)
            val invitation = tx.get(codeRef)
            check(account.getString("role") == "PADRE") { "Este vínculo es para cuentas padre / apoderado" }
            check(invitation.exists() && invitation.getString("usedBy").orEmpty().isEmpty()) { "Código no disponible" }
            val team = checkNotNull(invitation.getString("teamId"))
            val player = checkNotNull(invitation.getString("playerId"))
            val children = (account.get("children") as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value }?.toMutableMap() ?: mutableMapOf()
            val teamChildren = (children[team] as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value }?.toMutableMap() ?: mutableMapOf()
            teamChildren[player] = mapOf("name" to invitation.getString("name").orEmpty(), "code" to code)
            children[team] = teamChildren
            tx.update(userRef, mapOf("children" to children, "lastFamilyCode" to code))
            tx.update(codeRef, mapOf("usedBy" to uid, "usedAt" to FieldValue.serverTimestamp()))
            tx.update(db.collection("teams").document(team).collection("players").document(player), "parentUid", uid)
        }.await()
    }
}
