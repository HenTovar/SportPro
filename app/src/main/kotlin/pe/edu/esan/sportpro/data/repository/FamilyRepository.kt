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
        val uid = checkNotNull(auth.currentUser?.uid) { "Inicia sesión" }
        val code = FamilyPolicy.newCode()
        val ref = db.collection("familyCodes").document(code)
        val quota = db.collection("familyCodeQuotas").document(uid)
        db.runTransaction { tx ->
            check(!tx.get(ref).exists()) { "Vuelve a generar el código" }
            val before = tx.get(quota)
            val window = before.getTimestamp("windowStart")
            val restart = window == null || window.toDate().time + 3_600_000L <= System.currentTimeMillis()
            val count = if (restart) 1L else (before.getLong("count") ?: 20L) + 1L
            check(count <= 20L) { "Límite de 20 códigos por hora. Espera al siguiente período." }
            tx.set(ref, mapOf("teamId" to teamId, "playerId" to playerId, "issuedBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(), "expiresAt" to Timestamp(Date(System.currentTimeMillis() + 20 * 60_000)),
                "usedBy" to "", "usedAt" to null))
            tx.set(quota, mapOf("windowStart" to if (restart) FieldValue.serverTimestamp() else window,
                "count" to count, "lastCode" to code))
        }.await()
        return code
    }
    data class CleanupPlan(val teamId: String, val staleIds: List<String>, val legacyNameIds: List<String>)
    suspend fun previewCleanup(teamId: String): CleanupPlan {
        val uid = checkNotNull(auth.currentUser?.uid) { "Inicia sesión" }
        val admin = db.collection("users").document(uid).get().await().getString("role") == "ADMIN"
        var query: com.google.firebase.firestore.Query = db.collection("familyCodes").whereEqualTo("teamId", teamId)
        if (!admin) query = query.whereEqualTo("issuedBy", uid)
        val docs = query.limit(100).get().await().documents
        val now = System.currentTimeMillis()
        val stale = docs.filter { it.getString("usedBy").orEmpty().isNotBlank() || (it.getTimestamp("expiresAt")?.toDate()?.time ?: Long.MAX_VALUE) <= now }
        return CleanupPlan(teamId, stale.map { it.id }, if (admin) docs.filter { it !in stale && it.contains("name") }.map { it.id } else emptyList())
    }
    suspend fun cleanup(plan: CleanupPlan) {
        require(plan.staleIds.size + plan.legacyNameIds.size <= 100)
        val batch = db.batch()
        plan.staleIds.forEach { batch.delete(db.collection("familyCodes").document(it)) }
        plan.legacyNameIds.forEach { batch.update(db.collection("familyCodes").document(it), "name", FieldValue.delete()) }
        batch.commit().await()
    }
    suspend fun revoke(parentUid: String, teamId: String, playerId: String) {
        val actor = checkNotNull(auth.currentUser?.uid) { "Inicia sesión" }
        require(parentUid.isNotBlank() && teamId.isNotBlank() && playerId.isNotBlank())
        val user = db.collection("users").document(parentUid)
        val child = db.collection("teams").document(teamId).collection("players").document(playerId)
        db.runTransaction { tx ->
            val admin = tx.get(db.collection("users").document(actor))
            val account = tx.get(user)
            val player = tx.get(child)
            check(admin.getString("role") == "ADMIN" && (admin.getBoolean("active") ?: admin.getBoolean("isActive") ?: false)) {
                "Sólo un administrador activo puede revocar"
            }
            check(account.exists() && player.exists()) { "Cuenta o ficha inexistente; no se modificó nada" }
            val legacy = account.getString("teamId") == teamId && account.getString("playerId") == playerId
            val raw = account.get("children")
            val linked = (raw as? Map<*, *>)?.get(teamId) as? Map<*, *>
            val previousOwner = player.getString("parentUid")
            check(linked?.containsKey(playerId) == true || legacy || previousOwner == parentUid) { "El vínculo ya no existe; actualiza la ficha" }
            val fields = mutableMapOf<String, Any?>(
                "children" to FamilyPolicy.withoutChild(raw, teamId, playerId),
                "lastFamilyRevocation" to mapOf("teamId" to teamId, "playerId" to playerId,
                    "by" to actor, "at" to FieldValue.serverTimestamp())
            )
            if (legacy) { fields["teamId"] = ""; fields["playerId"] = "" }
            tx.update(user, fields)
            // Stale grants can be removed without harming the current, different family.
            tx.update(child, mapOf("parentUid" to if (previousOwner == parentUid) null else previousOwner,
                "familyRevokedAt" to FieldValue.serverTimestamp(), "familyRevokedParentUid" to parentUid))
        }.await()
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
            teamChildren[player] = mapOf("code" to code)
            children[team] = teamChildren
            tx.update(userRef, mapOf("children" to children, "lastFamilyCode" to code))
            tx.update(codeRef, mapOf("usedBy" to uid, "usedAt" to FieldValue.serverTimestamp(), "name" to FieldValue.delete()))
            tx.update(db.collection("teams").document(team).collection("players").document(player), "parentUid", uid)
        }.await()
        try { codeRef.delete().await() }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { /* Grant committed; staff cleanup can remove the name-free receipt later. */ }
    }
}
