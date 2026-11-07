package pe.edu.esan.sportpro.ui.screens.matches

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import pe.edu.esan.sportpro.data.model.*
import pe.edu.esan.sportpro.data.repository.MatchRepository
import pe.edu.esan.sportpro.di.DefaultAppContainer
import pe.edu.esan.sportpro.ui.navigation.safeBack
import pe.edu.esan.sportpro.ui.viewmodel.LiveMatchViewModel

@Composable
fun LiveMatchScreen(id:String,nav:NavHostController) {
    val vm:LiveMatchViewModel=viewModel(key="live-$id",factory=object:ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T:ViewModel> create(c:Class<T>):T =
            LiveMatchViewModel(id,MatchRepository(FirebaseFirestore.getInstance(),FirebaseAuth.getInstance()),DefaultAppContainer().authRepository) as T
    })
    val state by vm.state.collectAsState();val match=state.feed.match
    var editor by remember { mutableStateOf(false) };var editing by remember { mutableStateOf<MatchEvent?>(null) }
    var catalog by remember { mutableStateOf(false) };var summary by remember { mutableStateOf(false) }
    val staff=RolePolicy.isStaff(state.profile?.role)
    Scaffold(topBar={TopAppBar(title={Text(match?.name ?: "Partido en vivo")},navigationIcon={TextButton(onClick={nav.safeBack()}){Text("Volver")}})}) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {
                if(match==null) CircularProgressIndicator() else {
                    val score=MatchFacts.score(match,state.feed.events)
                    Text("Equipo A ${score.first} — ${score.second} Equipo B",style=MaterialTheme.typography.headlineMedium)
                    Text(when(match.status){"finished"->"Finalizado";"in_progress"->"En juego";else->"Programado"})
                }
                Text(if(state.feed.pending)"Hay cambios locales pendientes de sincronizar" else if(state.feed.cached)"Datos de caché: espera conexión para confirmar" else "Sincronizado en tiempo real")
                state.error?.let { Text(it,color=MaterialTheme.colorScheme.error);TextButton(onClick=vm::clearError){Text("Cerrar aviso")} }
            }
            if(staff&&match!=null)item {
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Button(onClick={editing=null;editor=true},enabled=!state.busy&&state.feed.types.any { it.active }){Text("Registrar evento")}
                    TextButton(onClick={catalog=true}){Text("Catálogo")}
                }
                Row {
                    TextButton(onClick={vm.setStatus("in_progress")},enabled=!state.busy){Text("Iniciar")}
                    TextButton(onClick={vm.setStatus("finished")},enabled=!state.busy){Text("Finalizar")}
                    TextButton(onClick={summary=true}){Text("Resumen IA")}
                }
                Text("Jugador y minuto pueden quedar desconocidos. No registres contactos ni datos privados en observaciones.",style=MaterialTheme.typography.bodySmall)
            }
            if(state.narrative.text.isNotBlank())item {
                val stale=state.narrative.sourceVersion!=match?.eventVersion||state.narrative.sourceHash!=MatchFacts.hash(state.feed.events)
                val approved=state.narrative.state=="APPROVED"&&!stale
                // Readers only see an approved, current text; staff can inspect a draft.
                if(staff||approved)Card {Column(Modifier.padding(12.dp)) {
                    Text(if(stale)"Resumen desactualizado: necesita otra revisión" else if(approved)"Resumen aprobado por el entrenador" else "Borrador IA: pendiente de revisión")
                    Text(state.narrative.text)
                }}
            }
            item {Text("Cronología (${state.feed.events.size})",style=MaterialTheme.typography.titleLarge)}
            items(state.feed.events,key={it.id}) { event ->
                Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(12.dp)) {
                    Text("${event.minute?.let { "$it'" } ?: "Minuto desconocido"} · ${event.typeLabel}${if(event.voided)" · ANULADO" else ""}")
                    Text("Equipo ${if(event.teamId==match?.teamA)"A" else "B"} · ${event.player.ifBlank { "Jugador desconocido" }}")
                    if(event.note.isNotBlank())Text(event.note)
                    Text("Revisión ${event.revision} · autor ${event.createdBy.take(8)} · último cambio ${event.updatedBy.take(8)}",style=MaterialTheme.typography.bodySmall)
                    Row {
                        TextButton(onClick={vm.history(event)}){Text("Historial")}
                        if(staff)TextButton(onClick={editing=event;editor=true},enabled=!state.busy&&!state.feed.cached&&!state.feed.pending){Text("Corregir / anular")}
                    }
                }}
            }
        }
    }
    if(editor&&match!=null)EventEditor(match,state.feed.types,editing,onDismiss={editor=false},onSave={type,minute,team,player,note,voided->
        vm.saveEvent(editing,type,minute,team,player,note,voided);editor=false
    })
    if(catalog)CatalogEditor(state.feed.types,{catalog=false},vm::saveType)
    state.history?.let { entries ->AlertDialog(onDismissRequest=vm::closeHistory,title={Text("Historial inmutable")},text={Column(Modifier.heightIn(max=400.dp).verticalScroll(rememberScrollState())) {
        entries.forEach { Text("${it.previous} → ${it.current}\nActor ${it.actorUid.take(12)} · ${java.util.Date(it.at)}") }
    }},confirmButton={TextButton(onClick=vm::closeHistory){Text("Cerrar")}}) }
    if(summary){var text by remember(state.narrative.text){mutableStateOf(state.narrative.text)}
        val current=state.narrative.sourceVersion==match?.eventVersion&&state.narrative.sourceHash==MatchFacts.hash(state.feed.events)
        AlertDialog(onDismissRequest={summary=false},title={Text("Revisión del entrenador")},text={Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState())) {
            Text("La IA sólo recibe los eventos registrados. Verifica hechos, omisiones y lenguaje antes de aprobar.")
            Button(onClick=vm::generate,enabled=!state.busy&&!state.feed.cached&&!state.feed.pending){Text("Generar borrador IA")}
            OutlinedTextField(value=text,onValueChange={text=it},label={Text("Borrador editable")},modifier=Modifier.fillMaxWidth(),minLines=4)
            if(!current&&text.isNotBlank())Text("Los eventos cambiaron: genera otra versión antes de aprobar.")
            state.error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
            if(state.busy)CircularProgressIndicator()
        }},confirmButton={TextButton(onClick={vm.saveNarrative(text,true)},enabled=current&&text.isNotBlank()&&!state.busy){Text("Aprobar")}},dismissButton={Row {
            TextButton(onClick={vm.saveNarrative(text,false)},enabled=text.isNotBlank()&&!state.busy){Text("Guardar revisión")}
            TextButton(onClick={summary=false}){Text("Cerrar")}
        }})
    }
}

@Composable
private fun EventEditor(match:LiveMatch,types:List<EventType>,old:MatchEvent?,onDismiss:()->Unit,
    onSave:(EventType,String,String,String,String,Boolean)->Unit) {
    var type by remember {mutableStateOf(types.find{it.id==old?.typeId} ?: types.firstOrNull {it.active})}
    var minute by remember {mutableStateOf(old?.minute?.toString().orEmpty())}
    var team by remember {mutableStateOf(old?.teamId ?: match.teamA)}
    var player by remember {mutableStateOf(old?.player.orEmpty())};var note by remember {mutableStateOf(old?.note.orEmpty())}
    var voided by remember {mutableStateOf(old?.voided ?: false)};var error by remember {mutableStateOf<String?>(null)}
    AlertDialog(onDismissRequest=onDismiss,title={Text(if(old==null)"Nuevo evento" else "Corregir sin borrar")},text={Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        types.filter {it.active||it.id==old?.typeId}.forEach { candidate ->Row {RadioButton(selected=type?.id==candidate.id,onClick={type=candidate});Text(candidate.label)} }
        OutlinedTextField(minute,{minute=it},label={Text("Minuto (vacío si desconocido)")},singleLine=true)
        Row {FilterChip(team==match.teamA,{team=match.teamA},label={Text("Equipo A")});Spacer(Modifier.width(8.dp));FilterChip(team==match.teamB,{team=match.teamB},label={Text("Equipo B")})}
        OutlinedTextField(player,{player=it},label={Text("Dorsal / jugador (opcional)")})
        OutlinedTextField(note,{note=it},label={Text("Observación (opcional)")})
        if(old!=null)Row {Checkbox(voided,{voided=it});Text("Anular: conserva el registro")}
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={TextButton(onClick={try {
        MatchFacts.validate(minute,team,match,note,player);val selected=requireNotNull(type){"Selecciona tipo"};onSave(selected,minute,team,player,note,voided)
    }catch(e:IllegalArgumentException){error=e.message}}){Text("Guardar")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancelar")}})
}

@Composable
private fun CatalogEditor(types:List<EventType>,onDismiss:()->Unit,onSave:(EventType)->Unit) {
    var selected by remember {mutableStateOf<EventType?>(null)};var label by remember {mutableStateOf("")};var goal by remember {mutableStateOf(false)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Catálogo en tiempo real")},text={Column(Modifier.heightIn(max=430.dp).verticalScroll(rememberScrollState())) {
        Text("Los cambios no alteran retroactivamente los eventos ya registrados.")
        types.forEach {type->Row {TextButton(onClick={selected=type;label=type.label;goal=type.countsGoal}){Text(type.label)}
            Switch(type.active,{onSave(type.copy(active=it))})}}
        OutlinedTextField(label,{label=it},label={Text("Nombre del tipo")})
        Row {Checkbox(goal,{goal=it});Text("Suma un gol al equipo elegido")}
        TextButton(onClick={selected=null;label="";goal=false}){Text("Nuevo tipo")}
    }},confirmButton={TextButton(onClick={onSave(EventType(selected?.id.orEmpty(),label,goal,selected?.active ?: true));label="";selected=null},enabled=label.isNotBlank()&&label.length<=40){Text("Guardar tipo")}},dismissButton={TextButton(onClick=onDismiss){Text("Cerrar")}})
}
