package pe.edu.esan.sportpro.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.*
import java.util.Date

/** All membership authorization and atomic effects are rechecked by Firestore rules. */
class GuidedAccountRepository(private val auth: FirebaseAuth, private val firestore: FirebaseFirestore) : GuidedAccounts {
    private fun uid() = checkNotNull(auth.currentUser?.uid) { "Inicia sesión" }
    private fun user(id: String) = firestore.collection("users").document(id)
    private fun personal(id: String) = firestore.collection("guidedProfiles").document(id)
    private fun player(team: String, id: String) = firestore.collection("teams").document(team).collection("players").document(id)
    private fun fields(id: String, p: GuidedProfile): Map<String, Any?> = mapOf(
        "uid" to id, "requestedRole" to p.requestedRole.name, "name" to p.name.trim(), "phone" to p.phone.trim(),
        "dateOfBirth" to p.dateOfBirth, "heightCm" to p.heightCm, "weightKg" to p.weightKg,
        "position" to p.position?.name, "dominantFoot" to p.dominantFoot,
        "specialtyOrLicense" to p.specialtyOrLicense.trim(), "yearsExperience" to p.yearsExperience)
    private fun decode(d: DocumentSnapshot, role: UserRole, fallbackName: String = "") = GuidedProfile(
        requestedRole = d.getString("requestedRole")?.let { runCatching { UserRole.valueOf(it) }.getOrNull() } ?: role,
        name = d.getString("name") ?: fallbackName, phone = d.getString("phone").orEmpty(),
        dateOfBirth = d.getLong("dateOfBirth"), heightCm = (d.get("heightCm") as? Number)?.toDouble(),
        weightKg = (d.get("weightKg") as? Number)?.toDouble(),
        position = d.getString("position")?.let { runCatching { PlayerPosition.valueOf(it) }.getOrNull() },
        dominantFoot = d.getString("dominantFoot"), specialtyOrLicense = d.getString("specialtyOrLicense").orEmpty(),
        yearsExperience = d.getLong("yearsExperience")?.toInt())
    override fun register(email: String, password: String, profile: GuidedProfile): Flow<Result<User>> = flow {
        try {
            profile.validate()
            RolePolicy.registrationBaseRole(profile.requestedRole) // rejects self-ADMIN
            AuthRepository(auth, firestore).register(email,password,profile.name.trim(),profile.requestedRole).collect { result ->
                if (result is Result.Success) {
                    personal(result.data.uid).set(fields(result.data.uid,profile)).await()
                }
                emit(result)
            }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { emit(Result.Error("No se completaron todos los datos. Inicia sesión para revisarlos; no se concedieron permisos adicionales.")) }
    }
    override suspend fun loadMyData(): GuidedProfile {
        val id=uid(); val u=user(id).get(Source.SERVER).await()
        val role=UserRole.valueOf(checkNotNull(u.getString("role")))
        var p=decode(personal(id).get(Source.SERVER).await(),role,u.getString("name").orEmpty())
        val team=u.getString("teamId").orEmpty();val pid=u.getString("playerId").orEmpty()
        if (role==UserRole.JUGADOR && team.isNotEmpty() && pid.isNotEmpty()) {
            val row=player(team,pid).get(Source.SERVER).await()
            p=p.copy(position=row.getString("position")?.let { PlayerPosition.valueOf(it) } ?: p.position,
                dominantFoot=row.getString("dominantFoot") ?: p.dominantFoot)
        }
        val pending=firestore.collection("roleRequests").document(id).get(Source.SERVER).await()
        val coachPending=pending.getString("status")=="PENDING" && pending.getString("toRole")=="ENTRENADOR"
        return p.copy(requestedRole=if(coachPending) UserRole.ENTRENADOR else role,coachPending=coachPending)
    }
    override suspend fun saveMyData(profile: GuidedProfile) {
        profile.validate(requireSports = false);val id=uid()
        firestore.runTransaction { tx ->
            val u=tx.get(user(id));val previous=tx.get(personal(id))
            // Sports are displayed from the current player row; retain original registration values in private profile.
            val values=fields(id,profile).toMutableMap()
            if (previous.exists()) for (key in listOf("position","dominantFoot","requestedRole")) values[key]=previous.get(key)
            tx.set(personal(id),values)
            tx.update(user(id),"name",profile.name.trim())
            val team=u.getString("teamId").orEmpty();val pid=u.getString("playerId").orEmpty()
            if(u.getString("role")=="JUGADOR" && team.isNotEmpty() && pid.isNotEmpty())
                tx.update(player(team,pid),mapOf("name" to profile.name.trim(),"updatedAt" to System.currentTimeMillis()))
        }.await()
    }
    override suspend fun generateTeamCode(teamId: String): String {
        require(teamId.isNotBlank());val actor=uid();val code=FamilyPolicy.newCode()
        val ref=firestore.collection("teamCodes").document(code);val q=firestore.collection("teamCodeQuotas").document(actor)
        firestore.runTransaction { tx ->
            check(!tx.get(ref).exists()) { "Vuelve a generar el código" }
            val old=tx.get(q);val start=old.getTimestamp("windowStart")
            val fresh=start==null || start.toDate().time+3_600_000L<=System.currentTimeMillis()
            val count=if(fresh) 1L else (old.getLong("count") ?: 20L)+1L
            check(count<=20) { "Límite de 20 emisiones por hora" }
            tx.set(ref,mapOf("teamId" to teamId,"issuedBy" to actor,"createdAt" to FieldValue.serverTimestamp(),
                "expiresAt" to Timestamp(Date(System.currentTimeMillis()+72*3_600_000L)),"count" to 0))
            tx.set(q,mapOf("windowStart" to if(fresh) FieldValue.serverTimestamp() else start,"count" to count,"lastCode" to code))
            tx.set(firestore.collection("teamJoinStates").document(teamId),mapOf("code" to code,"revoked" to false,
                "by" to actor,"at" to FieldValue.serverTimestamp()))
        }.await()
        return code
    }
    override suspend fun revokeTeamCode(teamId: String) {
        firestore.collection("teamJoinStates").document(teamId).update(mapOf("revoked" to true,
            "by" to uid(),"at" to FieldValue.serverTimestamp())).await()
    }
    override suspend fun joinTeam(code: String) {
        val id=uid();val normalized=FamilyPolicy.normalizeCode(code)
        require(FamilyPolicy.validCode(normalized)) { "Revisa el código de equipo" }
        val ref=firestore.collection("teamCodes").document(normalized)
        firestore.runTransaction { tx ->
            val u=tx.get(user(id));val gift=tx.get(ref);val p=tx.get(personal(id))
            check(u.getString("role")=="JUGADOR" && u.getString("teamId").orEmpty().isEmpty()) { "Primero retírate de tu equipo actual" }
            check(gift.exists() && p.exists()) { "Código o datos no disponibles" }
            val team=checkNotNull(gift.getString("teamId"));val pid=id+"_"+normalized
            val claim=ref.collection("claims").document(id);check(!tx.get(claim).exists()) { "Ya utilizaste este código" }
            check(!tx.get(player(team,pid)).exists()) { "La ficha ya existe" }
            val n=gift.getLong("count") ?: 40L;check(n<40) { "El código no tiene cupos" }
            tx.update(ref,mapOf("count" to n+1))
            tx.update(user(id),mapOf("teamId" to team,"playerId" to pid,"lastTeamCode" to normalized))
            tx.set(player(team,pid),mapOf("id" to pid,"teamId" to team,"userUid" to id,"name" to p.getString("name"),
                "position" to p.getString("position"),"dominantFoot" to p.getString("dominantFoot"),"active" to true,
                "joinCode" to normalized,"joinedAt" to FieldValue.serverTimestamp(),"createdAt" to System.currentTimeMillis(),"updatedAt" to System.currentTimeMillis()))
            tx.set(claim,mapOf("uid" to id,"teamId" to team,"playerId" to pid,"code" to normalized,"at" to FieldValue.serverTimestamp()))
        }.await()
    }
    private suspend fun exit(targetUid: String, expectedTeamId: String?, destination: String?) {
        val actor=uid();val event=firestore.collection("membershipActions").document()
        firestore.runTransaction { tx ->
            val u=tx.get(user(targetUid));val team=u.getString("teamId").orEmpty();val pid=u.getString("playerId").orEmpty()
            check(team.isNotEmpty() && pid.isNotEmpty() && (expectedTeamId==null || team==expectedTeamId)) { "El equipo cambió; actualiza la pantalla" }
            val old=tx.get(player(team,pid));val newTeam=destination.orEmpty();val newPid=if(newTeam.isEmpty()) "" else targetUid+"_"+event.id
            tx.update(user(targetUid),mapOf("teamId" to newTeam,"playerId" to newPid,"membershipActionId" to event.id))
            tx.update(player(team,pid),mapOf("active" to false,"retiredBy" to actor,"retiredAt" to FieldValue.serverTimestamp()))
            tx.set(event,mapOf("uid" to targetUid,"fromTeam" to team,"fromPlayer" to pid,"toTeam" to newTeam,
                "toPlayer" to newPid,"actor" to actor,"at" to FieldValue.serverTimestamp()))
            if(newTeam.isNotEmpty()) tx.set(player(newTeam,newPid),mapOf("id" to newPid,"teamId" to newTeam,"userUid" to targetUid,
                "name" to old.getString("name"),"position" to old.getString("position"),"dominantFoot" to (old.getString("dominantFoot") ?: "RIGHT"),
                "active" to true,"joinedAt" to FieldValue.serverTimestamp(),"createdAt" to System.currentTimeMillis(),"updatedAt" to System.currentTimeMillis()))
        }.await()
    }
    override suspend fun leaveTeam(expectedTeamId: String) = exit(uid(),expectedTeamId,null)
    override suspend fun movePlayerAsAdmin(userUid: String, targetTeamId: String) { require(targetTeamId.isNotBlank());exit(userUid,null,targetTeamId) }
    override suspend fun removePlayer(teamId: String, playerId: String) {
        val row=player(teamId,playerId).get(Source.SERVER).await();val id=checkNotNull(row.getString("userUid")) { "Ficha sin cuenta vinculada" }
        exit(id,teamId,null)
    }
    override suspend fun loadTeamJoiners(teamId: String): List<TeamJoiner> =
        firestore.collection("teams").document(teamId).collection("players").whereEqualTo("active",true).limit(100).get(Source.SERVER).await()
            .documents.mapNotNull { d -> d.getString("userUid")?.takeIf { it.isNotBlank() }?.let { TeamJoiner(it,d.id,d.getString("name").orEmpty(),d.getTimestamp("joinedAt")?.toDate()?.time ?: 0L) } }
    override suspend fun loadPlayerData(teamId: String, playerId: String): GuidedProfile {
        val row=player(teamId,playerId).get(Source.SERVER).await();val id=checkNotNull(row.getString("userUid")) { "Ficha sin cuenta" }
        return decode(personal(id).get(Source.SERVER).await(),UserRole.JUGADOR,row.getString("name").orEmpty()).copy(
            position=row.getString("position")?.let { PlayerPosition.valueOf(it) },dominantFoot=row.getString("dominantFoot"))
    }
    override suspend fun savePlayerSports(teamId: String, playerId: String, position: PlayerPosition, dominantFoot: String) {
        require(dominantFoot in listOf("RIGHT","LEFT","BOTH"))
        player(teamId,playerId).update(mapOf("position" to position.name,"dominantFoot" to dominantFoot,"updatedAt" to System.currentTimeMillis())).await()
    }
    override suspend fun savePlayerAsAdmin(teamId: String, playerId: String, profile: GuidedProfile) {
        profile.validate();val row=player(teamId,playerId).get(Source.SERVER).await();val id=checkNotNull(row.getString("userUid"))
        firestore.batch().apply {
            set(personal(id),fields(id,profile))
            update(user(id),"name",profile.name.trim())
            update(player(teamId,playerId),mapOf("name" to profile.name.trim(),"position" to checkNotNull(profile.position).name,
                "dominantFoot" to checkNotNull(profile.dominantFoot),"updatedAt" to System.currentTimeMillis()))
        }.commit().await()
    }
}
