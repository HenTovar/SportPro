package pe.edu.esan.sportpro.features

import com.google.firebase.firestore.*
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import pe.edu.esan.sportpro.data.model.*

/** Existing team queries only. Server rules remain the source of authorization. */
class TeamFeaturesRepository(private val db: FirebaseFirestore) {
    fun observe(team: String, staff: Boolean, player: String, alert: (LocalAlert)->Unit) = callbackFlow<FeatureState> {
        if(team.isBlank()) { trySend(FeatureState(loading=false)); close(); return@callbackFlow }
        val tracker=AlertTracker()
        var home=emptyList<LiveMatch>(); var away=emptyList<LiveMatch>(); var trainings=emptyList<Training>(); var attendance=emptyList<AttendanceRecord>()
        val eventRows=mutableMapOf<String,List<MatchEvent>>()
        val metadata=mutableMapOf<String,Pair<Boolean,Boolean>>()
        val listeners=mutableMapOf<String,ListenerRegistration>()
        fun emit() { trySend(FeatureState(team,(home+away).distinctBy { it.id },eventRows.toMap(),trainings,attendance,
            cached=!metadata.keys.containsAll(listOf("home","away","training","attendance")) || metadata.values.any { it.first },pending=metadata.values.any { it.second },
            loading=!metadata.keys.containsAll(listOf("home","away","training","attendance")) || (home+away).any { it.id !in eventRows })) }
        fun fail() { trySend(FeatureState(team=team,loading=false,error="No se pudo confirmar el acceso o cargar los datos. Recarga tu sesión.")); close() }
        fun meta(key: String,s: QuerySnapshot) { metadata[key]=s.metadata.isFromCache to s.metadata.hasPendingWrites() }
        fun confirmed(s: QuerySnapshot)=!s.metadata.isFromCache && !s.metadata.hasPendingWrites()
        fun trackMatches() {
            val rows=(home+away).distinctBy { it.id }; val ids=rows.map { it.id }.toSet()
            listeners.keys.filter { it.startsWith("event/") && it.substringAfter("event/") !in ids }.toList().forEach { key ->
                listeners.remove(key)?.remove(); eventRows.remove(key.substringAfter("event/")); tracker.forgetMatch(key.substringAfter("event/")); metadata.remove(key)
            }
            rows.forEach { match -> val key="event/"+match.id
                if(key !in listeners) listeners[key]=db.collection("matches").document(match.id).collection("events")
                    .addSnapshotListener(MetadataChanges.INCLUDE) { s,e ->
                        if((home+away).none { it.id==match.id }) return@addSnapshotListener
                        if(e!=null) fail() else if(s!=null) {
                            val events=s.documents.map(::event);eventRows[match.id]=events;meta(key,s)
                            tracker.events(match.id,events,confirmed(s)).forEach(alert);emit()
                        }
                    }
            }
            val confirmedLists=metadata["home"]== (false to false) && metadata["away"]== (false to false)
            tracker.matches(rows,confirmedLists).forEach(alert);emit()
        }
        listeners["home"]=db.collection("matches").whereEqualTo("teamA",team).addSnapshotListener(MetadataChanges.INCLUDE) { s,e ->
            if(e!=null)fail() else if(s!=null){home=s.documents.filter { it.getBoolean("active")!=false }.map(::match);meta("home",s);trackMatches()}
        }
        listeners["away"]=db.collection("matches").whereEqualTo("teamB",team).addSnapshotListener(MetadataChanges.INCLUDE) { s,e ->
            if(e!=null)fail() else if(s!=null){away=s.documents.filter { it.getBoolean("active")!=false }.map(::match);meta("away",s);trackMatches()}
        }
        listeners["training"]=db.collection("teams").document(team).collection("trainings").addSnapshotListener(MetadataChanges.INCLUDE) { s,e ->
            if(e!=null)fail() else if(s!=null){trainings=s.documents.map(::training);meta("training",s);tracker.trainings(trainings,confirmed(s)).forEach(alert);emit()}
        }
        if(staff || player.isNotBlank()) {
            val base=db.collection("teams").document(team).collection("attendance")
            val query: Query=if(staff)base else base.whereEqualTo("playerId",player)
            listeners["attendance"]=query.addSnapshotListener(MetadataChanges.INCLUDE){s,e ->
                if(e!=null)fail() else if(s!=null){attendance=s.documents.map(::attendance);meta("attendance",s);emit()}
            }
        } else { metadata["attendance"]=false to false }
        awaitClose { listeners.values.toList().forEach { it.remove() };listeners.clear() }
    }
    companion object {
        fun millis(value: Any?): Long = when(value){ is Timestamp -> value.toDate().time;is Number -> value.toLong();else -> 0L }
        private fun match(d:DocumentSnapshot)=LiveMatch(d.id,d.getString("name")?:"Partido",d.getString("teamA").orEmpty(),d.getString("teamB").orEmpty(),d.getString("status")?:"not_started",millis(d.get("date")),d.getLong("eventVersion")?:0)
        private fun event(d:DocumentSnapshot)=MatchEvent(id=d.id,typeId=d.getString("typeId").orEmpty(),typeLabel=d.getString("typeLabel").orEmpty(),countsGoal=d.getBoolean("countsGoal")?:false,
            minute=d.getLong("minute")?.toInt(),teamId=d.getString("teamId").orEmpty(),player=d.getString("player").orEmpty(),voided=d.getBoolean("voided")?:false,
            revision=d.getLong("revision")?:0,recordedAt=millis(d.get("createdAt")))
        private fun training(d:DocumentSnapshot)=Training(id=d.id,teamId=d.getString("teamId").orEmpty(),name=d.getString("name")?:"Entrenamiento",date=millis(d.get("date")),
            duration=d.getLong("duration")?.toInt()?:90,exercises=(d.get("exercises") as? List<*>)?.filterIsInstance<String>().orEmpty(),
            objective=d.getString("objective").orEmpty(),description=d.getString("description").orEmpty(),status=runCatching { TrainingStatus.valueOf(d.getString("status")?:"PROGRAMADO") }.getOrDefault(TrainingStatus.PROGRAMADO),updatedAt=millis(d.get("updatedAt")))
        private fun attendance(d:DocumentSnapshot)=AttendanceRecord(d.id,d.getString("playerId").orEmpty(),d.getString("trainingId").orEmpty(),
            runCatching { AttendanceStatus.valueOf(d.getString("status")?:"") }.getOrDefault(AttendanceStatus.EXCUSED),recordedAt=millis(d.get("recordedAt")))
    }
}
