package pe.edu.esan.sportpro.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class CommunityPost(val id: String,val text: String,val authorUid: String,val state: String,val at: Long)
data class CommunityComment(val id: String,val text: String,val authorUid: String,val state: String)
data class CommunityThread(val comments: List<CommunityComment> = emptyList(),val likes: Int = 0,val liked: Boolean = false)
data class CommunityReport(val reporter: String,val reason: String)
class CommunityRepository(private val db: FirebaseFirestore,private val auth: FirebaseAuth) {
    private fun uid()=requireNotNull(auth.currentUser?.uid) { "Inicia sesión" }
    private fun posts(team: String)=db.collection("teams").document(team).collection("posts")
    private fun valid(text: String,max: Int=1000) { require(text.isNotBlank()&&text.length<=max) { "Texto obligatorio, máximo $max caracteres" } }
    fun watch(team: String,moderator: Boolean)=callbackFlow<Result<List<CommunityPost>>> {
        val query: Query=if(moderator)posts(team) else posts(team).whereEqualTo("state","VISIBLE")
        val listener=query.addSnapshotListener { snap,error ->
            if(error!=null)trySend(Result.Error(error.message ?: "No se pudo leer la comunidad"))
            else trySend(Result.Success(snap?.documents.orEmpty().map {
                CommunityPost(it.id,it.getString("text").orEmpty(),it.getString("authorUid").orEmpty(),
                    it.getString("state") ?: "VISIBLE",it.getTimestamp("createdAt")?.toDate()?.time ?: 0)
            }.sortedByDescending { it.at }))
        }
        awaitClose { listener.remove() }
    }
    suspend fun post(team: String,text: String) {
        valid(text);val actor=uid()
        posts(team).add(mapOf("text" to text.trim(),"authorUid" to actor,"state" to "VISIBLE",
            "createdAt" to FieldValue.serverTimestamp(),"updatedAt" to FieldValue.serverTimestamp(),"updatedBy" to actor)).await()
    }
    suspend fun edit(team: String,id: String,text: String) {
        valid(text);posts(team).document(id).update(mapOf("text" to text.trim(),"updatedAt" to FieldValue.serverTimestamp(),"updatedBy" to uid())).await()
    }
    suspend fun moderate(team: String,id: String,hide: Boolean) {
        posts(team).document(id).update(mapOf("state" to if(hide)"HIDDEN" else "VISIBLE","updatedBy" to uid(),"updatedAt" to FieldValue.serverTimestamp())).await()
    }
    fun thread(team: String,id: String,moderator: Boolean)=callbackFlow<Result<CommunityThread>> {
        val ref=posts(team).document(id);var state=CommunityThread()
        val comments: Query=if(moderator)ref.collection("comments") else ref.collection("comments").whereEqualTo("state","VISIBLE")
        val a=comments.addSnapshotListener { s,e ->
            if(e!=null)trySend(Result.Error(e.message ?: "No se pudieron leer comentarios"))
            else {state=state.copy(comments=s?.documents.orEmpty().map { CommunityComment(it.id,it.getString("text").orEmpty(),it.getString("authorUid").orEmpty(),it.getString("state") ?: "VISIBLE") });trySend(Result.Success(state))}
        }
        val b=ref.collection("reactions").addSnapshotListener { s,e ->
            if(e!=null)trySend(Result.Error(e.message ?: "No se pudieron leer reacciones"))
            else {state=state.copy(likes=s?.size() ?: 0,liked=s?.documents.orEmpty().any { it.id==auth.currentUser?.uid });trySend(Result.Success(state))}
        }
        awaitClose { a.remove();b.remove() }
    }
    suspend fun comment(team: String,id: String,text: String) {
        valid(text,500);val actor=uid()
        posts(team).document(id).collection("comments").add(mapOf("text" to text.trim(),"authorUid" to actor,
            "state" to "VISIBLE","createdAt" to FieldValue.serverTimestamp(),"updatedAt" to FieldValue.serverTimestamp(),"updatedBy" to actor)).await()
    }
    suspend fun hideComment(team: String,id: String,commentId: String) {
        posts(team).document(id).collection("comments").document(commentId).update(mapOf("state" to "HIDDEN","updatedBy" to uid(),"updatedAt" to FieldValue.serverTimestamp())).await()
    }
    suspend fun react(team: String,id: String,alreadyLiked: Boolean) {
        val actor=uid();val ref=posts(team).document(id).collection("reactions").document(actor)
        if(alreadyLiked)ref.delete().await() else ref.set(mapOf("kind" to "LIKE","createdAt" to FieldValue.serverTimestamp())).await()
    }
    suspend fun report(team: String,id: String,reason: String) {
        valid(reason,300);posts(team).document(id).collection("reports").document(uid()).set(mapOf("reason" to reason.trim(),"createdAt" to FieldValue.serverTimestamp())).await()
    }
    suspend fun reports(team: String,id: String)=posts(team).document(id).collection("reports").get().await().documents.map {
        CommunityReport(it.id,it.getString("reason").orEmpty())
    }
}
