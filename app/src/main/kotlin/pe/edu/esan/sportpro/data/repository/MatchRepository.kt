package pe.edu.esan.sportpro.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.*
import java.util.UUID

/** Listeners follow the class pattern; removal is tied to the collector lifecycle. */
class MatchRepository(private val db: FirebaseFirestore, private val auth: FirebaseAuth) {
    private fun uid() = requireNotNull(auth.currentUser?.uid) { "Inicia sesión" }
    private fun root(id: String) = db.collection("matches").document(id)
    private fun millis(value: Any?): Long = when(value) {
        is com.google.firebase.Timestamp -> value.toDate().time
        is Number -> value.toLong()
        else -> 0
    }
    private fun match(d: DocumentSnapshot) = LiveMatch(d.id,d.getString("name") ?: "Partido",
        d.getString("teamA").orEmpty(),d.getString("teamB").orEmpty(),
        d.getString("status") ?: "not_started",millis(d.get("date")),d.getLong("eventVersion") ?: 0)
    private fun event(d: DocumentSnapshot) = MatchEvent(d.id,d.getString("typeId").orEmpty(),
        d.getString("typeLabel") ?: d.getString("type").orEmpty(), d.getBoolean("countsGoal") ?: false,
        d.getLong("minute")?.toInt(),d.getString("teamId").orEmpty(),d.getString("player").orEmpty(),
        d.getString("note").orEmpty(),d.getBoolean("voided") ?: false,d.getLong("revision") ?: 0,
        d.getString("revisionId").orEmpty(),d.getString("createdBy").orEmpty(),
        d.getString("updatedBy").orEmpty(),millis(d.get("createdAt")))
    fun matches(teamId: String) = callbackFlow<Result<List<LiveMatch>>> {
        var home: List<LiveMatch> = emptyList(); var away: List<LiveMatch> = emptyList()
        fun emit() { trySend(Result.Success((home+away).distinctBy { it.id }.sortedByDescending { it.date })) }
        val a = db.collection("matches").whereEqualTo("teamA",teamId).addSnapshotListener { snap,error ->
            if(error!=null) trySend(Result.Error(error.message ?: "No se pudieron leer partidos"))
            else { home=snap?.documents.orEmpty().filter { it.getBoolean("active") != false }.map(::match);emit() }
        }
        val b = db.collection("matches").whereEqualTo("teamB",teamId).addSnapshotListener { snap,error ->
            if(error!=null) trySend(Result.Error(error.message ?: "No se pudieron leer partidos"))
            else { away=snap?.documents.orEmpty().filter { it.getBoolean("active") != false }.map(::match);emit() }
        }
        awaitClose { a.remove();b.remove() }
    }
    fun feed(id: String) = callbackFlow<Result<MatchFeed>> {
        var state=MatchFeed()
        fun emit() { trySend(Result.Success(state)) }
        val a=root(id).addSnapshotListener(MetadataChanges.INCLUDE) { snap,error ->
            if(error!=null) trySend(Result.Error(error.message ?: "No se pudo leer el partido"))
            else if(snap!=null&&snap.exists()) { state=state.copy(match=match(snap));emit() }
            else trySend(Result.Error("Partido no encontrado"))
        }
        val b=root(id).collection("events").addSnapshotListener(MetadataChanges.INCLUDE) { snap,error ->
            if(error!=null) trySend(Result.Error(error.message ?: "No se pudieron leer los eventos"))
            else if(snap!=null) { state=state.copy(events=MatchFacts.timeline(snap.documents.map(::event)),
                cached=snap.metadata.isFromCache,pending=snap.metadata.hasPendingWrites());emit() }
        }
        val c=root(id).collection("eventTypes").addSnapshotListener { snap,error ->
            if(error!=null) trySend(Result.Error(error.message ?: "No se pudo leer el catálogo"))
            else { state=state.copy(types=snap?.documents.orEmpty().map {
                EventType(it.id,it.getString("label").orEmpty(),it.getBoolean("countsGoal") ?: false,it.getBoolean("active") ?: true)
            }.sortedBy { it.label });emit() }
        }
        awaitClose { a.remove();b.remove();c.remove() }
    }
    suspend fun create(teamA: String, teamB: String, name: String) {
        require(teamA.isNotBlank()&&teamB.isNotBlank()&&teamA!=teamB) { "Elige dos equipos distintos" }
        require(name.isNotBlank()&&name.length<=100) { "Nombre obligatorio (hasta 100 caracteres)" }
        val actor=uid();val ref=db.collection("matches").document();val batch=db.batch()
        batch.set(ref,mapOf("teamA" to teamA,"teamB" to teamB,"name" to name.trim(),"date" to FieldValue.serverTimestamp(),
            "status" to "not_started","eventVersion" to 0L,"active" to true,"createdBy" to actor))
        listOf("Inicio","Fin","Gol","Falta","Tarjeta amarilla","Tarjeta roja","Cambio","Penal","Tiro de esquina","Saque lateral","Saque de meta","Fuera de juego").forEachIndexed { index,label ->
            batch.set(ref.collection("eventTypes").document("type$index"),mapOf("label" to label,
                "countsGoal" to (label=="Gol"),"active" to true,"updatedBy" to actor,"updatedAt" to FieldValue.serverTimestamp()))
        }
        batch.commit().await()
    }
    suspend fun status(id: String,status: String) {
        require(status in listOf("not_started","in_progress","finished"))
        root(id).update(mapOf("status" to status,"eventVersion" to FieldValue.increment(1),"updatedBy" to uid(),"updatedAt" to FieldValue.serverTimestamp())).await()
    }
    suspend fun type(id: String,type: EventType) {
        require(type.label.isNotBlank()&&type.label.length<=40)
        val ref=if(type.id.isBlank())root(id).collection("eventTypes").document() else root(id).collection("eventTypes").document(type.id)
        ref.set(mapOf("label" to type.label.trim(),"countsGoal" to type.countsGoal,"active" to type.active,
            "updatedBy" to uid(),"updatedAt" to FieldValue.serverTimestamp())).await()
    }
    /** New events can queue offline; corrections use a transaction and require connection. */
    suspend fun add(id: String,type: EventType,minute: Int?,team: String,player: String,note: String) {
        val actor=uid();val ref=root(id).collection("events").document();val revision=UUID.randomUUID().toString()
        val data=mapOf("typeId" to type.id,"typeLabel" to type.label,"countsGoal" to type.countsGoal,
            "minute" to minute,"teamId" to team,"player" to player.trim(),"note" to note.trim(),
            "voided" to false,"revision" to 0L,"revisionId" to revision,"createdBy" to actor,
            "updatedBy" to actor,"createdAt" to FieldValue.serverTimestamp(),"updatedAt" to FieldValue.serverTimestamp())
        val batch=db.batch();batch.set(ref,data)
        batch.update(root(id),mapOf("eventVersion" to FieldValue.increment(1),"lastEventId" to ref.id))
        batch.set(ref.collection("revisions").document(revision),mapOf("actorUid" to actor,"at" to FieldValue.serverTimestamp(),"previousData" to emptyMap<String,Any>(),"eventData" to data))
        batch.commit().await()
    }
    suspend fun correct(id: String,old: MatchEvent,type: EventType,minute: Int?,team: String,player: String,note: String,voided: Boolean) {
        val actor=uid();val ref=root(id).collection("events").document(old.id);val revision=UUID.randomUUID().toString()
        db.runTransaction { tx ->
            val snap=tx.get(ref);require(snap.exists()) { "Evento no encontrado" }
            require(snap.getLong("revision")==old.revision) { "El evento cambió en otro dispositivo; vuelve a abrirlo" }
            val before=snap.data.orEmpty();val after=mutableMapOf<String,Any?>().apply { putAll(before) }
            after.putAll(mapOf("typeId" to type.id,"typeLabel" to if(type.id==old.typeId)old.typeLabel else type.label,
                "countsGoal" to if(type.id==old.typeId)old.countsGoal else type.countsGoal,"minute" to minute,
                "teamId" to team,"player" to player.trim(),"note" to note.trim(),"voided" to voided,
                "revision" to (old.revision+1),"revisionId" to revision,"updatedBy" to actor,"updatedAt" to FieldValue.serverTimestamp()))
            tx.update(root(id),mapOf("eventVersion" to FieldValue.increment(1),"lastEventId" to ref.id))
            tx.set(ref,after);tx.set(ref.collection("revisions").document(revision),mapOf("actorUid" to actor,
                "at" to FieldValue.serverTimestamp(),"previousData" to before,"eventData" to after))
        }.await()
    }
    suspend fun history(id: String,eventId: String): List<EventRevision> = root(id).collection("events").document(eventId)
        .collection("revisions").get().await().documents.map {
            val before=it.get("previousData") as? Map<*,*>;val after=it.get("eventData") as? Map<*,*>
            EventRevision(it.id,it.getString("actorUid").orEmpty(),millis(it.get("at")),before?.get("typeLabel")?.toString() ?: "Nuevo",
                "${after?.get("typeLabel")} · ${if(after?.get("voided")==true) "anulado" else "vigente"}")
        }.sortedBy { it.at }
    fun narrative(id: String,staff: Boolean) = callbackFlow<Result<Narrative>> {
        val listener=root(id).collection("aiSummaries").document(if(staff)"draft" else "published").addSnapshotListener { d,error ->
            if(error!=null) trySend(Result.Error(error.message ?: "No se pudo leer el resumen"))
            else trySend(Result.Success(Narrative(d?.getString("text").orEmpty(),d?.getString("sourceHash").orEmpty(),
                d?.getString("state") ?: "DRAFT",d?.getString("authorUid").orEmpty(),d?.getString("approvedBy").orEmpty(),d?.getLong("sourceVersion") ?: 0)))
        }
        awaitClose { listener.remove() }
    }
    suspend fun confirmedFacts(id: String): Pair<LiveMatch,List<MatchEvent>> {
        val before=root(id).get(Source.SERVER).await(); val events=root(id).collection("events").get(Source.SERVER).await()
        val after=root(id).get(Source.SERVER).await()
        require(!before.metadata.hasPendingWrites() && !after.metadata.hasPendingWrites() && !events.metadata.hasPendingWrites()) { "Espera a sincronizar los cambios" }
        require(before.getLong("eventVersion")==after.getLong("eventVersion")) { "El partido cambió; vuelve a generar" }
        return match(after) to events.documents.map(::event)
    }
    suspend fun saveNarrative(id: String,text: String,hash: String,version: Long,approve: Boolean) {
        require(text.isNotBlank()&&text.length<=8000)
        val actor=uid();val data=mapOf("text" to text,
            "sourceHash" to hash,"sourceVersion" to version,"state" to if(approve) "APPROVED" else "DRAFT","authorUid" to actor,
            "approvedBy" to if(approve)actor else "","updatedAt" to FieldValue.serverTimestamp())
        val batch=db.batch();batch.set(root(id).collection("aiSummaries").document("draft"),data)
        if(approve)batch.set(root(id).collection("aiSummaries").document("published"),data)
        batch.commit().await()
    }
}
