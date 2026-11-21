package pe.edu.esan.sportpro.ui.screens.trials

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.*
import pe.edu.esan.sportpro.features.UiInteractionPolicy
import pe.edu.esan.sportpro.data.repository.OpenTrialsRepository
import pe.edu.esan.sportpro.data.repository.Result
import pe.edu.esan.sportpro.data.repository.TeamRepository
import pe.edu.esan.sportpro.ui.navigation.LocalTeamSession

@Composable
fun OpenTrialsScreen(user: User, onBack: () -> Unit) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val repository = remember(user.uid) { OpenTrialsRepository(firestore,FirebaseAuth.getInstance()) }
    val fromCache by repository.isFromCache.collectAsState()
    val session = LocalTeamSession.current
    val scope = rememberCoroutineScope()
    var loadedTeams by remember(user.uid) { mutableStateOf<List<Team>>(emptyList()) }
    var teamError by remember(user.uid) { mutableStateOf<String?>(null) }
    val teams = (if(session.account?.uid == user.uid && session.teams.isNotEmpty()) session.teams else loadedTeams)
        .filter { it.isActive && OpenTrialsPolicy.canPropose(user,it.id) }
    val staff = user.isActive && user.role in listOf(UserRole.ADMIN,UserRole.ENTRENADOR)
    var mode by remember(user.uid) { mutableStateOf("published") }
    var teamId by remember(user.uid) { mutableStateOf("") }
    var notices by remember(user.uid) { mutableStateOf<List<OpenTrial>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var filters by remember { mutableStateOf(OpenTrialFilters()) }
    var editor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<OpenTrial?>(null) }
    var moderation by remember { mutableStateOf<Pair<OpenTrial,Boolean>?>(null) }

    LaunchedEffect(user.uid,user.isActive,user.role,user.coachTeamIds) {
        if(staff) when(val result = TeamRepository(firestore) { user }.getAllTeams().first { it !is Result.Loading }) {
            is Result.Success -> { loadedTeams=result.data;teamError=null }
            is Result.Error -> teamError=result.message
            else -> Unit
        }
    }
    LaunchedEffect(teams.map { it.id }) {
        if(teams.none { it.id == teamId }) teamId=teams.firstOrNull()?.id.orEmpty()
    }
    LaunchedEffect(user.uid,user.isActive,user.role,user.coachTeamIds,mode,teamId,refresh) {
        notices=emptyList();error=null;loading=true
        if(!user.isActive) { error="Cuenta inactiva.";loading=false;return@LaunchedEffect }
        if(!staff && mode!="published") { mode="published";return@LaunchedEffect }
        if(mode == "team" && teamId.isBlank()) { loading=false;return@LaunchedEffect }
        val flow = when(mode) {
            "pending" -> repository.watchPending()
            "team" -> repository.watchTeam(teamId)
            else -> repository.watchPublished()
        }
        flow.collect { result -> when(result) {
            is Result.Loading -> loading=true
            is Result.Error -> { loading=false;notices=emptyList();error=result.message }
            is Result.Success -> { loading=false;notices=result.data;error=null }
        } }
    }
    fun perform(successMessage: String, action: suspend () -> Result<*>) {
        scope.launch {
            busy=true;error=null;message=null
            try {
                when(val result=action()) {
                    is Result.Error -> error=result.message
                    is Result.Success -> { message=successMessage;editor=false;editing=null;moderation=null }
                    else -> Unit
                }
            } finally { busy=false }
        }
    }
    val filterError = filters.validate()
    val visible = if(filterError == null) OpenTrialsPolicy.filter(notices,filters) else emptyList()
    Scaffold(topBar={ TopAppBar(title={Text("Pruebas abiertas")},navigationIcon={
        IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Volver")}
    }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {
                Text("Encuentra la próxima oportunidad",style=MaterialTheme.typography.headlineSmall)
                Text("Avisos institucionales revisados por administración. No se publican contactos ni datos personales.")
                if(fromCache) Text("Datos guardados en este dispositivo o cambios aún sin confirmar. La lista puede estar desactualizada.",
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick={refresh++},enabled=!busy) { Text("Recargar avisos") }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    FilterChip(mode=="published",{mode="published"},label={Text("Publicados")})
                    if(user.role == UserRole.ADMIN && user.isActive)
                        FilterChip(mode=="pending",{mode="pending"},label={Text("Por revisar")})
                    if(staff) FilterChip(mode=="team",{mode="team"},label={Text("Mi equipo")})
                }
                if(mode=="team") TrialChoice("Equipo",teams.firstOrNull { it.id==teamId }?.name ?: "Sin equipo",
                    teams.map { it.id to it.name }) { teamId=it }
                if(staff) Button(onClick={editing=null;editor=true},enabled=teams.isNotEmpty()&&!busy,
                    modifier=Modifier.fillMaxWidth()) { Text("Proponer aviso") }
                if(staff && teams.isEmpty()) Text(teamError ?: "No hay equipos activos asignados para proponer avisos.")
            }
            item { ElevatedCard { Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Filtrar avisos",style=MaterialTheme.typography.titleMedium)
                TrialChoice<TeamCategory?>("Categoría",filters.category?.let(UiInteractionPolicy::categoryLabel) ?: "Todas",
                    listOf(null to "Todas")+TeamCategory.entries.map { it to UiInteractionPolicy.categoryLabel(it) }) { filters=filters.copy(category=it) }
                TrialChoice("Posición",filters.position.label,TrialPosition.entries.map { it to it.label }) { filters=filters.copy(position=it) }
                OutlinedTextField(filters.from,{filters=filters.copy(from=it)},label={Text("Desde · AAAA-MM-DD")},
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Ascii), singleLine=true,isError=filterError!=null,modifier=Modifier.fillMaxWidth())
                OutlinedTextField(filters.to,{filters=filters.copy(to=it)},label={Text("Hasta · AAAA-MM-DD")},
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Ascii), singleLine=true,isError=filterError!=null,modifier=Modifier.fillMaxWidth())
                OutlinedTextField(filters.location,{filters=filters.copy(location=it)},label={Text("Lugar")},
                    singleLine=true,modifier=Modifier.fillMaxWidth())
                filterError?.let { Text(it,color=MaterialTheme.colorScheme.error) }
                TextButton(onClick={filters=OpenTrialFilters()}) { Text("Limpiar filtros") }
            } } }
            if(loading || busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            error?.let { item { Text(it,color=MaterialTheme.colorScheme.error) } }
            message?.let { item { Text(it,color=MaterialTheme.colorScheme.primary) } }
            if(!loading && error==null && filterError==null && visible.isEmpty() && (mode!="team" || teamId.isNotBlank())) item {
                Text(if(fromCache) "No hay avisos en la copia disponible con estos filtros; falta confirmación del servidor."
                    else if(mode=="pending") "No hay propuestas por revisar con estos filtros." else "No hay avisos con estos filtros.")
            }
            items(visible,key={it.id}) { trial ->
                ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(trial.draft.title,style=MaterialTheme.typography.titleLarge)
                    Text("${UiInteractionPolicy.categoryLabel(trial.draft.category)} · ${trial.draft.position.label}")
                    Text("${OpenTrialDates.format(trial.draft.dateMillis)} · ${trial.draft.location}")
                    Text(trial.draft.description)
                    Text(when(trial.state) { TrialState.PENDING -> "Pendiente de aprobación";TrialState.PUBLISHED -> "Publicado";TrialState.REJECTED -> "Rechazado: puedes corregir y reenviar" })
                    if(OpenTrialsPolicy.canRevise(user,trial)) TextButton(onClick={editing=trial;editor=true},enabled=!busy) { Text("Editar y enviar a revisión") }
                    if(OpenTrialsPolicy.canModerate(user,trial)) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Button(onClick={moderation=trial to true},enabled=!busy) { Text("Aprobar") }
                        OutlinedButton(onClick={moderation=trial to false},enabled=!busy) { Text("Rechazar") }
                    }
                } }
            }
        }
    }
    if(editor) TrialEditor(teams,editing,teamId,busy,error,onDismiss={if(!busy){editor=false;editing=null}},onSave={draft ->
        val previous=editing
        perform("Propuesta enviada a administración. Aún no está publicada.") { if(previous==null) repository.propose(user,draft)
            else repository.revise(user,previous.id,previous.revision,draft) }
    })
    moderation?.let { (trial,publish) -> AlertDialog(onDismissRequest={if(!busy)moderation=null},
        title={Text(if(publish)"Aprobar publicación" else "Rechazar propuesta")},
        text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(trial.draft.title)
            Text(if(publish)"Confirma que el aviso sólo contiene información institucional y no incluye datos personales, datos de menores ni contactos. Será visible para las cuentas activas registradas."
                else "El aviso no se publicará. El personal del equipo podrá corregirlo y volver a enviarlo.")
            error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        }},confirmButton={TextButton(onClick={perform(if(publish) "Aviso aprobado y publicado." else "Propuesta rechazada. El personal del equipo puede corregirla y reenviarla.") { repository.moderate(user,trial.id,trial.revision,publish) }},enabled=!busy){Text("Confirmar")}},
        dismissButton={TextButton(onClick={moderation=null},enabled=!busy){Text("Cancelar")}})
    }
}

@Composable
private fun <T> TrialChoice(label: String, value: String, options: List<Pair<T,String>>, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth(),enabled=options.isNotEmpty()) { Text("$label: $value") }
        DropdownMenu(expanded,onDismissRequest={expanded=false}) {
            options.forEach { (key,text) -> DropdownMenuItem(text={Text(text)},onClick={expanded=false;onSelect(key)}) }
        }
    }
}

@Composable
private fun TrialEditor(teams: List<Team>, editing: OpenTrial?, preferredTeam: String, busy: Boolean,
    serverError: String?, onDismiss: () -> Unit, onSave: (OpenTrialDraft) -> Unit) {
    var team by remember(editing?.id) { mutableStateOf(editing?.draft?.teamId ?: preferredTeam.ifBlank { teams.firstOrNull()?.id.orEmpty() }) }
    var title by remember(editing?.id) { mutableStateOf(editing?.draft?.title.orEmpty()) }
    var category by remember(editing?.id) { mutableStateOf(editing?.draft?.category ?: teams.firstOrNull { it.id==team }?.category ?: TeamCategory.SUB_15) }
    var position by remember(editing?.id) { mutableStateOf(editing?.draft?.position ?: TrialPosition.ANY) }
    var date by remember(editing?.id) { mutableStateOf(editing?.draft?.dateMillis?.let(OpenTrialDates::format).orEmpty()) }
    var place by remember(editing?.id) { mutableStateOf(editing?.draft?.location.orEmpty()) }
    var description by remember(editing?.id) { mutableStateOf(editing?.draft?.description.orEmpty()) }
    var localError by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest=onDismiss,title={Text(if(editing==null)"Proponer prueba abierta" else "Editar propuesta")},
        text={Column(Modifier.heightIn(max=500.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Publica sólo información institucional. No incluyas nombres personales, datos de menores, teléfonos, correos ni enlaces de contacto.")
            if(editing?.state==TrialState.PUBLISHED) Text("Al guardar, el aviso dejará de estar publicado hasta una nueva aprobación.")
            TrialChoice("Equipo",teams.firstOrNull { it.id==team }?.name ?: "Sin equipo",
                teams.filter { editing==null || it.id==editing.draft.teamId }.map { it.id to it.name }) {
                team=it;teams.firstOrNull { item -> item.id==it }?.let { item -> category=item.category }
            }
            OutlinedTextField(title,{title=it},label={Text("Título · máximo 120")},modifier=Modifier.fillMaxWidth())
            TrialChoice("Categoría",UiInteractionPolicy.categoryLabel(category),TeamCategory.entries.map { it to UiInteractionPolicy.categoryLabel(it) }) { category=it }
            TrialChoice("Posición",position.label,TrialPosition.entries.map { it to it.label }) { position=it }
            OutlinedTextField(date,{date=it},label={Text("Fecha · AAAA-MM-DD")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Ascii),singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(place,{place=it},label={Text("Lugar institucional · máximo 160")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(description,{description=it},label={Text("Descripción · máximo 1500")},minLines=3,modifier=Modifier.fillMaxWidth())
            (localError ?: serverError)?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        }},confirmButton={TextButton(enabled=!busy,onClick={
            val millis=OpenTrialDates.parse(date)
            if(millis==null) localError="Fecha inválida. Usa AAAA-MM-DD."
            else {
                val draft=OpenTrialDraft(team,title,category,position,millis,place,description)
                localError=OpenTrialsPolicy.validate(draft)
                if(localError==null) onSave(draft)
            }
        }) { Text(if(busy)"Guardando…" else "Enviar a revisión") }},dismissButton={TextButton(onClick=onDismiss,enabled=!busy){Text("Cancelar")}})
}
