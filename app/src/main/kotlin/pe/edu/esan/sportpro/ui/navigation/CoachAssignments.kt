package pe.edu.esan.sportpro.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Visible only in ADMIN profile; rules enforce every scope update independently. */
@Composable
fun CoachAssignments() {
    val db=remember {FirebaseFirestore.getInstance()};val scope=rememberCoroutineScope()
    var coaches by remember {mutableStateOf<List<com.google.firebase.firestore.DocumentSnapshot>>(emptyList())}
    var teams by remember {mutableStateOf<List<com.google.firebase.firestore.DocumentSnapshot>>(emptyList())}
    var selected by remember {mutableStateOf("")};var assigned by remember {mutableStateOf(setOf<String>())}
    var busy by remember {mutableStateOf(false)};var message by remember {mutableStateOf("")};var revision by remember {mutableIntStateOf(0)}
    var confirm by remember {mutableStateOf(false)}
    LaunchedEffect(revision) {
        busy=true
        try {
            teams=db.collection("teams").get().await().documents
            coaches=db.collection("users").whereEqualTo("role","ENTRENADOR").get().await().documents
        } catch(e:CancellationException){throw e} catch(_:Exception){message="No se pudieron cargar las asignaciones; requiere ADMIN activo."}
        finally{busy=false}
    }
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text("Entrenadores por equipo",style=MaterialTheme.typography.titleMedium)
        Text("ADMIN asigna hasta 20 equipos. Guardar vacío quita también el acceso heredado; no elimina equipos.")
        coaches.forEach {coach->TextButton(enabled=!busy,onClick={
            selected=coach.id
            assigned=if(coach.contains("coachTeamIds")) (coach.get("coachTeamIds") as? List<*>)?.filterIsInstance<String>()?.toSet().orEmpty()
                else (teams.filter{it.getString("ownerId")==coach.id}.map{it.id}+coach.getString("teamId").orEmpty()).filter{it.isNotBlank()}.toSet()
        }) {Text(coach.getString("name").orEmpty()+" · "+coach.getString("email").orEmpty())}}
        if(selected.isNotBlank()) {
            Text("Equipos de "+coaches.find{it.id==selected}?.getString("name").orEmpty())
            teams.forEach {team->Row {Checkbox(team.id in assigned,enabled=!busy,onCheckedChange={checked->
                assigned=if(checked)assigned+team.id else assigned-team.id
            });Text(team.getString("name") ?: "Equipo")}}
            OutlinedButton(enabled=!busy&&assigned.size<=20,onClick={confirm=true}) {Text("Guardar "+assigned.size+" asignaciones")}
        }
        if(message.isNotBlank())Text(message)
    }}
    if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("Confirmar acceso del entrenador")},
        text={Text("Se conservarán exactamente "+assigned.size+" equipos. Los que no estén seleccionados dejarán de autorizarse en el servidor.")},
        confirmButton={TextButton(onClick={confirm=false;busy=true;scope.launch {
            try {
                val actor=checkNotNull(FirebaseAuth.getInstance().currentUser?.uid)
                db.collection("users").document(selected).update(mapOf("coachTeamIds" to assigned.sorted(),"coachScopeBy" to actor,"coachScopeAt" to FieldValue.serverTimestamp())).await()
                message="Asignaciones guardadas. El entrenador debe actualizar su sesión.";revision++
            } catch(e:CancellationException){throw e} catch(_:Exception){message="No se confirmó el cambio de asignaciones."} finally{busy=false}
        }}) {Text("Confirmar")}},dismissButton={TextButton(onClick={confirm=false}){Text("Cancelar")}})
}
