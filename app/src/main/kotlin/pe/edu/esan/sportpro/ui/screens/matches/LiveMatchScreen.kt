package pe.edu.esan.sportpro.ui.screens.matches

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import pe.edu.esan.sportpro.features.ScreenActionPolicy
import pe.edu.esan.sportpro.features.UiInteractionPolicy
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.Preview
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
import pe.edu.esan.sportpro.ui.viewmodel.EventDraft
import pe.edu.esan.sportpro.ui.viewmodel.SubmissionProgress
import pe.edu.esan.sportpro.ui.viewmodel.SubmissionPhase
import pe.edu.esan.sportpro.ui.common.*
import pe.edu.esan.sportpro.ui.theme.SportProTheme

internal fun eventEditableTeams(profile: User?, match: LiveMatch?): List<String> {
    if (profile == null || match == null || !profile.isActive || profile.uid.isBlank()) return emptyList()
    return listOf(match.teamA, match.teamB).filter { it.isNotBlank() }.distinct().filter { teamId ->
        profile.role == UserRole.ADMIN || (profile.role == UserRole.ENTRENADOR && teamId in profile.coachTeamIds)
    }
}

@Composable
fun LiveMatchScreen(id:String,nav:NavHostController,openEvent:Boolean=false,openPreparation:Boolean=false) {
    val vm:LiveMatchViewModel=viewModel(key="live-$id",factory=object:ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T:ViewModel> create(c:Class<T>):T =
            LiveMatchViewModel(id,MatchRepository(FirebaseFirestore.getInstance(),FirebaseAuth.getInstance()),DefaultAppContainer().authRepository) as T
    })
    val state by vm.state.collectAsState();val match=state.feed.match
    var editor by remember {mutableStateOf(false)};var editing by remember {mutableStateOf<MatchEvent?>(null)}
    var editorSession by remember { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    var recoveredDraft by remember { mutableStateOf<EventDraft?>(null) }
    val openEditor: (MatchEvent?)->Unit = { old ->
        editing=old;recoveredDraft=null;editorSession=java.util.UUID.randomUUID().toString();editor=true
    }
    val editorWrite=state.eventWrites[editorSession]
    var catalog by remember {mutableStateOf(false)};var summary by remember {mutableStateOf(false)}
    var preparationOpen by rememberSaveable(id) { mutableStateOf(false) }
    var academicTestData by remember { mutableStateOf(false) }
    var finishConfirmation by remember { mutableStateOf(false) }
    val teamNames = pe.edu.esan.sportpro.ui.navigation.LocalTeamSession.current.teams.associate { it.id to it.name }
    val teamAName = match?.teamA?.let { teamNames[it] }?.takeIf { it.isNotBlank() } ?: "Equipo A"
    val teamBName = match?.teamB?.let { teamNames[it] }?.takeIf { it.isNotBlank() } ?: "Equipo B"
    val staff=RolePolicy.isStaff(state.profile?.role)
    val editableTeams = eventEditableTeams(state.profile, match)
    LaunchedEffect(editorSession,editorWrite?.progress?.id,editorWrite?.progress?.phase) {
        val receipt=editorWrite ?: return@LaunchedEffect
        if(receipt.acceptedForEditor) editor=false
        else if(receipt.progress.phase==SubmissionPhase.REJECTED && !editor && !summary && !catalog && !preparationOpen) {
            // A queue receipt may be followed by a server denial. Restore only this
            // editor; receipts belonging to another editor remain recoverable below.
            editing=receipt.draft.old;recoveredDraft=receipt.draft;editor=true
        }
    }
    var openedFromShortcut by rememberSaveable(id) {mutableStateOf(false)}
    var preparationShortcutOpened by rememberSaveable(id) { mutableStateOf(false) }
    LaunchedEffect(openPreparation, match?.id, state.profile?.uid) {
        if (ScreenActionPolicy.openPreparation(openPreparation, preparationShortcutOpened, match != null, state.profile != null)) {
            preparationShortcutOpened = true
            preparationOpen = true
        }
    }
    LaunchedEffect(openEvent,staff,match?.id,state.feed.types,editableTeams) {
        if(openEvent&&!openPreparation&&!openedFromShortcut&&staff&&editableTeams.isNotEmpty()&&match!=null&&state.feed.types.any {it.active}) {
            openedFromShortcut=true;openEditor(null)
        }
    }
    Scaffold(topBar={TopAppBar(title={Text("Centro del partido")},navigationIcon={BackButton {nav.safeBack()}})}) { padding ->
        LazyColumn(Modifier.padding(padding),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item {
                if(match!=null)MatchBroadcastHeader(match,state.feed.events,teamAName,teamBName)
                else if(state.error==null)LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                if(match!=null)StatusBadge(if(state.feed.pending)"Cambios por sincronizar" else if(state.feed.cached)"Sin confirmar · caché" else "Sincronizado en tiempo real",
                    if(state.feed.pending||state.feed.cached)BadgeTone.Warning else BadgeTone.Positive)
                state.error?.let {Spacer(Modifier.height(12.dp));MessageNotice(it,true);TextButton(onClick=vm::clearError,modifier=Modifier.heightIn(min=48.dp)){Text("Cerrar aviso")}}
            }
            if(match!=null && state.profile!=null)item {
                OutlinedButton(onClick={preparationOpen=true}, modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
                    Text("Convocatoria y alineación")
                }
            }
            if(staff&&match!=null)item {
                ElevatedCard(elevation=CardDefaults.elevatedCardElevation(defaultElevation=2.dp)) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("Mesa de control",style=MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Button(onClick={openEditor(null)},enabled=!state.busy&&editableTeams.isNotEmpty()&&state.feed.types.any {it.active},modifier=Modifier.weight(1f).heightIn(min=48.dp)) {Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp));Text("Registrar evento")}
                        OutlinedButton(onClick={catalog=true},modifier=Modifier.heightIn(min=48.dp)){Text("Tipos de evento")}
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        if (UiInteractionPolicy.canStart(match.status))
                            TextButton(onClick={vm.setStatus("in_progress")},enabled=!state.busy,modifier=Modifier.heightIn(min=48.dp)){Text("Iniciar partido")}
                        if (UiInteractionPolicy.canFinish(match.status))
                            TextButton(onClick={finishConfirmation=true},enabled=!state.busy,modifier=Modifier.heightIn(min=48.dp)){Text("Finalizar partido")}
                        if (match.status == "finished") Text("Partido finalizado", style=MaterialTheme.typography.labelLarge)
                        TextButton(onClick={summary=true},modifier=Modifier.heightIn(min=48.dp)){Text("Revisar resumen")}
                    }
                    Text("¿Falta un dato? Puedes dejar minuto o jugador desconocidos. Evita datos privados en las observaciones.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }}
            }
            if(state.narrative.text.isNotBlank())item {
                val stale=state.narrative.sourceVersion!=match?.eventVersion||state.narrative.sourceHash!=MatchFacts.hash(state.feed.events)
                val approved=state.narrative.state=="APPROVED"&&!stale
                if(staff||approved)ElevatedCard {Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    StatusBadge(if(stale)"Necesita nueva revisión" else if(approved)"Aprobado por el entrenador" else "Borrador · revisión pendiente",if(approved)BadgeTone.Positive else BadgeTone.Warning)
                    Text("La historia del partido",style=MaterialTheme.typography.titleMedium)
                    Text(state.narrative.text,style=MaterialTheme.typography.bodyLarge)
                }}
            }
            state.eventWrites.values.forEach { write ->
                when(write.progress.phase) {
                    SubmissionPhase.QUEUED -> item(key="queued-${write.key}") { MessageNotice("Evento guardado en este dispositivo; pendiente de sincronizar.") }
                    SubmissionPhase.REJECTED, SubmissionPhase.UNCERTAIN -> if(!editor || editorSession!=write.key) item(key="recover-${write.key}") {
                        MessageNotice(write.progress.message ?: "Conservamos el borrador del evento.",write.progress.phase==SubmissionPhase.REJECTED)
                        TextButton(onClick={editing=write.draft.old;recoveredDraft=write.draft;editorSession=write.key;editor=true}) { Text("Recuperar borrador del evento") }
                    }
                    else -> Unit
                }
            }
            item {SectionHeading("Minuto a minuto","${state.feed.events.size} registros · las anulaciones conservan su historial")}
            if(match!=null&&state.feed.events.isEmpty())item {EmptyState(Icons.Default.PlayArrow,"El partido está por escribirse",if(staff)"Registra el primer evento desde la mesa de control." else "Aquí aparecerán las jugadas que registre el entrenador.")}
            items(state.feed.events,key={it.id}) {event->MatchTimelineCard(event,match?.teamA.orEmpty(),event.teamId in editableTeams,
                !state.busy&&!state.feed.cached&&!state.feed.pending,{vm.history(event)},{if(event.teamId in editableTeams)openEditor(event)},teamAName,teamBName)}
        }
    }
    if(preparationOpen && match!=null && state.profile!=null)
        MatchPreparationScreen(match, checkNotNull(state.profile)) { preparationOpen=false }
    if(editor&&match!=null&&editableTeams.isNotEmpty()&&(editing?.let { it.teamId in editableTeams } ?: true)) key(editorSession) {
        EventEditor(match,state.feed.types,editing,editableTeams,onDismiss={editor=false},onSave={type,minute,team,player,note,voided->
            if(team in editableTeams&&(editing?.let { it.teamId in editableTeams } ?: true))
                vm.saveEvent(editing,type,minute,team,player,note,voided,editorSession)
        },teamAName=teamAName,teamBName=teamBName,initialDraft=recoveredDraft,progress=editorWrite?.progress ?: SubmissionProgress())
    }
    if (finishConfirmation) AlertDialog(onDismissRequest={if(!state.busy)finishConfirmation=false},
        title={Text("¿Finalizar el partido?")},text={Text("Se marcará como finalizado. Revisa los eventos antes de confirmar.")},
        confirmButton={TextButton(enabled=!state.busy && UiInteractionPolicy.canFinish(match?.status.orEmpty()),onClick={
            if(finishConfirmation && !state.busy && UiInteractionPolicy.canFinish(match?.status.orEmpty())) {
                finishConfirmation=false;vm.setStatus("finished")
            }
        }){Text("Sí, finalizar partido")}},
        dismissButton={TextButton(enabled=!state.busy,onClick={finishConfirmation=false}){Text("Seguir revisando")}})
    if(catalog)CatalogEditor(state.feed.types,{catalog=false},vm::saveType)
    state.history?.let {entries->AlertDialog(onDismissRequest=vm::closeHistory,title={Text("Historial del evento")},text={Column(Modifier.heightIn(max=400.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        entries.forEach {Text("${it.previous} → ${it.current}\nActor ${it.actorUid.take(12)} · ${java.util.Date(it.at)}")}
    }},confirmButton={TextButton(onClick=vm::closeHistory){Text("Cerrar")}})}
    if(summary) {
        var text by remember(state.narrative.text){mutableStateOf(state.narrative.text)}
        val current=state.narrative.sourceVersion==match?.eventVersion&&state.narrative.sourceHash==MatchFacts.hash(state.feed.events)
        AlertDialog(onDismissRequest={summary=false},title={Text("Revisión del entrenador")},text={Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("La IA recibe tipos, minutos, equipos anónimos y alias; los nombres se reponen aquí. Sin IA se usa una plantilla. Revisa antes de aprobar.")
            state.generationNotice?.let { MessageNotice(it) }
            Text("IA opcional: sólo con datos de prueba",style=MaterialTheme.typography.labelLarge)
            Row(verticalAlignment=Alignment.CenterVertically) {
                Checkbox(checked=academicTestData,enabled=!state.busy,onCheckedChange={academicTestData=it})
                Text("Confirmo que son datos ficticios para uso académico",modifier=Modifier.weight(1f))
            }
            Button(onClick={val confirmed=academicTestData;academicTestData=false;vm.generate(testDataConfirmed=confirmed)},enabled=!state.busy){Text(if(academicTestData) "Generar IA o plantilla" else "Generar plantilla local")}
            OutlinedTextField(text,{text=it},label={Text("Borrador editable")},modifier=Modifier.fillMaxWidth(),minLines=4)
            if(!current&&text.isNotBlank())MessageNotice("El partido cambió. Genera otra versión antes de aprobar.")
            state.error?.let {MessageNotice(it,true)}
            if(state.busy)CircularProgressIndicator()
        }},confirmButton={TextButton(onClick={vm.saveNarrative(text,true)},enabled=current&&state.canPersistNarrative&&text.isNotBlank()&&!state.busy){Text("Aprobar resumen")}},dismissButton={Column {
            TextButton(onClick={vm.saveNarrative(text,false)},enabled=state.canPersistNarrative&&text.isNotBlank()&&!state.busy){Text("Guardar borrador")}
            TextButton(onClick={summary=false}){Text("Cerrar")}
        }})
    }
}

@Composable
fun MatchBroadcastHeader(match:LiveMatch,events:List<MatchEvent>,teamAName:String="Equipo A",teamBName:String="Equipo B") {
    val score=MatchFacts.score(match,events)
    SportHero(when(match.status){"in_progress"->"EN VIVO";"finished"->"PARTIDO FINALIZADO";else->"PRÓXIMO ENCUENTRO"},match.name,"Marcador de eventos vigentes") {
        Column(Modifier.fillMaxWidth().padding(vertical=12.dp).semantics(mergeDescendants=true) {
            contentDescription="Marcador: $teamAName ${score.first}, $teamBName ${score.second}";liveRegion=LiveRegionMode.Polite
        },horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text(teamAName,style=MaterialTheme.typography.labelLarge,color=Color.White)
                Text(teamBName,style=MaterialTheme.typography.labelLarge,color=Color.White)
            }
            AnimatedContent(targetState=score,label="marcador") {value->Text("${value.first} : ${value.second}",style=MaterialTheme.typography.displayLarge,color=Color.White)}
        }
        Text("Los eventos anulados no suman",style=MaterialTheme.typography.bodySmall,color=Color.White)
    }
}

@Composable
fun MatchTimelineCard(event:MatchEvent,teamA:String,staff:Boolean=false,canEdit:Boolean=false,onHistory:()->Unit={},onEdit:()->Unit={},teamAName:String="Equipo A",teamBName:String="Equipo B") {
    val name=event.typeLabel.lowercase()
    val icon=when {event.voided->Icons.Default.Close;event.countsGoal->Icons.Default.Star;name.contains("tarjeta")->Icons.Default.Warning;name.contains("cambio")->Icons.Default.Refresh;else->Icons.Default.Info}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Column(Modifier.width(52.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.primaryContainer) {Icon(icon,null,Modifier.padding(14.dp).size(24.dp),tint=MaterialTheme.colorScheme.onPrimaryContainer)}
            Text(event.minute?.let {"$it′"} ?: "s/m",style=MaterialTheme.typography.titleMedium,modifier=Modifier.semantics {contentDescription=event.minute?.let {"Minuto $it"} ?: "Minuto desconocido"})
        }
        ElevatedCard(Modifier.weight(1f).animateContentSize(),elevation=CardDefaults.elevatedCardElevation(defaultElevation=2.dp)) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(event.typeLabel,style=MaterialTheme.typography.titleMedium)
            if(event.voided)StatusBadge("Anulado · conservado",BadgeTone.Danger)
            Text("${if(event.teamId==teamA)teamAName else teamBName} · ${event.player.ifBlank {"Jugador desconocido"}}",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(event.note.isNotBlank())Text(event.note)
            Text("Revisión ${event.revision} · ${event.updatedBy.take(8)}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick=onHistory,modifier=Modifier.heightIn(min=48.dp)){Text("Ver historial")}
            if(staff)OutlinedButton(onClick=onEdit,enabled=canEdit,modifier=Modifier.heightIn(min=48.dp)){Icon(Icons.Default.Edit,null);Spacer(Modifier.width(6.dp));Text("Corregir / anular")}
        }}
    }
}

@Preview(showBackground=true,widthDp=390)
@Composable private fun ScorePreview() {SportProTheme {MatchBroadcastHeader(LiveMatch(name="Jornada de práctica",teamA="a",teamB="b",status="in_progress"),listOf(MatchEvent(id="g",teamId="a",countsGoal=true)))}}

@Composable
private fun EventEditor(match:LiveMatch,types:List<EventType>,old:MatchEvent?,allowedTeamIds:List<String>,onDismiss:()->Unit,
    onSave:(EventType,String,String,String,String,Boolean)->Unit,teamAName:String="Equipo A",teamBName:String="Equipo B",
    initialDraft:EventDraft?=null,progress:SubmissionProgress=SubmissionProgress()) {
    var type by remember {mutableStateOf(initialDraft?.type ?: types.find{it.id==old?.typeId} ?: types.firstOrNull {it.active})}
    var minute by remember {mutableStateOf(initialDraft?.minute ?: old?.minute?.toString().orEmpty())}
    var team by remember {mutableStateOf(initialDraft?.team?.takeIf {it in allowedTeamIds} ?: old?.teamId?.takeIf {it in allowedTeamIds} ?: allowedTeamIds.firstOrNull().orEmpty())}
    var player by remember {mutableStateOf(initialDraft?.player ?: old?.player.orEmpty())};var note by remember {mutableStateOf(initialDraft?.note ?: old?.note.orEmpty())}
    var voided by remember {mutableStateOf(initialDraft?.voided ?: old?.voided ?: false)};var error by remember {mutableStateOf<String?>(null)}
    AlertDialog(onDismissRequest={if(progress.phase!=SubmissionPhase.SAVING)onDismiss()},title={Text(if(old==null)"Nuevo evento" else "Corregir sin borrar")},text={Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        types.filter {it.active||it.id==old?.typeId}.forEach { candidate ->Row {RadioButton(selected=type?.id==candidate.id,enabled=!progress.blocksSend,onClick={type=candidate});Text(candidate.label)} }
        OutlinedTextField(minute,{minute=it},label={Text("Minuto (vacío si desconocido)")},singleLine=true,enabled=!progress.blocksSend,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
        Row {if(match.teamA in allowedTeamIds)FilterChip(team==match.teamA,{team=match.teamA},label={Text(teamAName)},enabled=!progress.blocksSend);Spacer(Modifier.width(8.dp));if(match.teamB in allowedTeamIds)FilterChip(team==match.teamB,{team=match.teamB},label={Text(teamBName)},enabled=!progress.blocksSend)}
        OutlinedTextField(player,{player=it},label={Text("Dorsal / jugador (opcional)")},enabled=!progress.blocksSend)
        OutlinedTextField(note,{note=it},label={Text("Observación (opcional)")},enabled=!progress.blocksSend)
        if(old!=null)Row {Checkbox(voided,{voided=it},enabled=!progress.blocksSend);Text("Anular: conserva el registro")}
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        progress.message?.let {MessageNotice(it,progress.phase==SubmissionPhase.REJECTED)}
    }},confirmButton={TextButton(enabled=!progress.blocksSend,onClick={try {
        require(team in allowedTeamIds && (old==null || old.teamId in allowedTeamIds)) {"El equipo ya no está en tu alcance"}
        MatchFacts.validate(minute,team,match,note,player);val selected=requireNotNull(type){"Selecciona tipo"};error=null;onSave(selected,minute,team,player,note,voided)
    }catch(e:IllegalArgumentException){error=e.message}}){Text(if(progress.phase==SubmissionPhase.SAVING)"Guardando…" else if(progress.phase==SubmissionPhase.UNCERTAIN)"Sin confirmar" else "Guardar evento")}},dismissButton={TextButton(onClick=onDismiss,enabled=progress.phase!=SubmissionPhase.SAVING){Text(if(progress.phase==SubmissionPhase.UNCERTAIN)"Cerrar sin reenviar" else "Cancelar")}})
}

@Composable
private fun CatalogEditor(types:List<EventType>,onDismiss:()->Unit,onSave:(EventType)->Unit) {
    var selected by remember {mutableStateOf<EventType?>(null)};var label by remember {mutableStateOf("")};var goal by remember {mutableStateOf(false)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Tipos de evento del partido")},text={Column(Modifier.heightIn(max=430.dp).verticalScroll(rememberScrollState())) {
        Text("Los cambios no alteran retroactivamente los eventos ya registrados.")
        types.forEach {type->Row {TextButton(onClick={selected=type;label=type.label;goal=type.countsGoal}){Text(type.label)}
            Switch(type.active,{onSave(type.copy(active=it))})}}
        OutlinedTextField(label,{label=it},label={Text("Nombre del tipo")})
        Row {Checkbox(goal,{goal=it});Text("Suma un gol al equipo elegido")}
        TextButton(onClick={selected=null;label="";goal=false}){Text("Nuevo tipo")}
    }},confirmButton={TextButton(onClick={onSave(EventType(selected?.id.orEmpty(),label,goal,selected?.active ?: true));label="";selected=null},enabled=label.isNotBlank()&&label.length<=40){Text("Guardar tipo de evento")}},dismissButton={TextButton(onClick=onDismiss){Text("Cerrar")}})
}
