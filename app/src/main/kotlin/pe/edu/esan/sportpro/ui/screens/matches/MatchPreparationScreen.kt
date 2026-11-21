package pe.edu.esan.sportpro.ui.screens.matches

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import pe.edu.esan.sportpro.features.UiInteractionPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import pe.edu.esan.sportpro.data.model.*
import pe.edu.esan.sportpro.data.repository.*
import pe.edu.esan.sportpro.di.DefaultAppContainer

private data class PreparationUiState(
    val teamId: String = "", val snapshot: PreparationSnapshot? = null,
    val players: List<Player> = emptyList(), val playersLoading: Boolean = false,
    val busy: Boolean = false, val error: String? = null, val message: String? = null
)

private class PreparationViewModel(private val match: LiveMatch, private val user: User,
                                   private val repo: MatchPreparationRepository,
                                   private val players: PlayerRepository) : ViewModel() {
    private val mutable = MutableStateFlow(PreparationUiState())
    val state = mutable.asStateFlow()
    private var watchJob: Job? = null
    private var playersJob: Job? = null
    fun manage(team: String) = user.isActive && RolePolicy.isStaff(user.role) && RolePolicy.canReadTeam(user, team)
    fun select(team: String) {
        stop()
        mutable.value = PreparationUiState(teamId = team, playersLoading = manage(team))
        if (team !in listOf(match.teamA, match.teamB) || !RolePolicy.canReadTeam(user, team)) {
            mutable.update { it.copy(error = "No tienes acceso a la preparación de este equipo.") }; return
        }
        watchJob = viewModelScope.launch {
            repo.watch(match.id, team).collect { result -> when (result) {
                is Result.Success -> mutable.update { it.copy(snapshot = result.data) }
                is Result.Error -> mutable.update { it.copy(snapshot = null, error = result.message) }
                else -> Unit
            } }
        }
        if (manage(team)) playersJob = viewModelScope.launch {
            players.getPlayersByTeam(team).collect { result -> when (result) {
                is Result.Success -> mutable.update { it.copy(players = result.data, playersLoading = false) }
                is Result.Error -> mutable.update { it.copy(error = result.message, playersLoading = false) }
                else -> Unit
            } }
        }
    }
    fun stop() { watchJob?.cancel(); playersJob?.cancel() }
    private fun action(message: String, work: suspend () -> Unit, done: () -> Unit = {}) {
        if (mutable.value.busy) return
        viewModelScope.launch {
            mutable.update { it.copy(busy = true, error = null, message = null) }
            try {
                val completed = withTimeoutOrNull(15_000) { work(); true }
                check(completed == true) { "No se confirmó el cambio. Revisa la información actual antes de reintentar." }
                mutable.update { it.copy(message = message) }; done()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                mutable.update { it.copy(error = e.message ?: "No se confirmó el cambio. Vuelve a cargar la preparación.") }
            } finally { mutable.update { it.copy(busy = false) } }
        }
    }
    fun invite(player: Player, invited: Boolean) {
        val s = mutable.value; val revision = s.snapshot?.preparation?.revision ?: return
        action(if (invited) "Convocatoria guardada. Puedes esperar la respuesta o registrarla en su nombre." else "Jugador retirado de esta convocatoria.", {
            require(manage(s.teamId)) { "Sólo el entrenador del equipo puede convocar" }
            repo.invite(match.id, s.teamId, player.id, player.name, invited, revision)
        })
    }
    fun respond(playerId: String, response: Availability, recordedByCoach: Boolean = false) {
        val s = mutable.value; val revision = s.snapshot?.preparation?.revision ?: return
        action(if (recordedByCoach) "Respuesta registrada por el entrenador." else "Respuesta guardada.", {
            require(MatchPreparationPolicy.canRecordResponse(user, s.teamId, playerId, recordedByCoach)) { "No tienes permiso para registrar esta respuesta" }
            repo.respond(match.id, s.teamId, playerId, response, revision, recordedByCoach)
        })
    }
    fun lineup(starters: List<String>, substitutes: List<String>, positions: Map<String, String>, revision: Long, done: () -> Unit) {
        val team = mutable.value.teamId
        action("Alineación guardada.", {
            require(manage(team)) { "Sólo el entrenador del equipo puede guardar la alineación" }
            repo.saveLineup(match.id, team, starters, substitutes, positions, revision)
        }, done)
    }
}

/** Full-screen dialog keeps the match and its own listeners in the same room. */
@Composable
fun MatchPreparationScreen(match: LiveMatch, user: User, onClose: () -> Unit) {
    val vm: PreparationViewModel = viewModel(key = "preparation-${match.id}-${user.uid}", factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PreparationViewModel(match, user,
                MatchPreparationRepository(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance()),
                DefaultAppContainer().playerRepository) as T
    })
    val state by vm.state.collectAsState()
    val teams = listOf(match.teamA, match.teamB).distinct().filter { RolePolicy.canReadTeam(user, it) }
    val teamNames = pe.edu.esan.sportpro.ui.navigation.LocalTeamSession.current.teams.associate { it.id to it.name }
    var selectedTeam by remember(match.id, user.uid) { mutableStateOf(teams.firstOrNull().orEmpty()) }
    var tab by remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedTeam) { if (selectedTeam.isNotBlank()) vm.select(selectedTeam) }
    DisposableEffect(vm) { onDispose { vm.stop() } }
    val preparation = state.snapshot?.preparation
    val canWrite = preparation != null && preparation.teamId == selectedTeam && state.error == null && !state.playersLoading && state.snapshot?.cached == false && state.snapshot?.pending == false && !state.busy
    val staff = vm.manage(selectedTeam)
    val ownIds = FamilyPolicy.children(user).filter { it.teamId == selectedTeam }.map { it.playerId }.toSet()

    val lineupStates = rememberSaveableStateHolder()
    var dirtyTeams by rememberSaveable(match.id, user.uid) { mutableStateOf(emptyList<String>()) }
    var discardConfirmation by remember { mutableStateOf(false) }
    fun requestClose() { if (!state.busy) { if (dirtyTeams.isEmpty()) onClose() else discardConfirmation = true } }

    Dialog(onDismissRequest = { requestClose() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Preparar partido", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { requestClose() }, enabled = !state.busy) { Text("Volver") }
                }
                Text(match.name, Modifier.padding(horizontal = 16.dp))
                if (teams.isEmpty()) Text("No tienes un equipo autorizado en este partido.", Modifier.padding(16.dp))
                if (teams.size > 1) Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    teams.forEach { team -> FilterChip(selectedTeam == team, { if (!state.busy) selectedTeam = team },
                        label = { Text(teamNames[team]?.takeIf { it.isNotBlank() } ?: if (team == match.teamA) "Equipo A" else "Equipo B") }) }
                }
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Convocatoria") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Alineación") })
                }
                state.error?.let {
                    Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { vm.select(selectedTeam) }, enabled = !state.busy) { Text("Volver a cargar preparación") }
                }
                state.message?.let { Text(it, Modifier.padding(horizontal = 16.dp)) }
                if (state.busy || (state.error == null && (preparation == null || state.playersLoading))) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (state.snapshot?.cached == true || state.snapshot?.pending == true)
                    Text("Información sin confirmar. Conéctate para convocar, responder o guardar.", Modifier.padding(16.dp))
                if (preparation != null) {
                    if (tab == 0) {
                        val current = state.players.associateBy { it.id }
                        val ids = if (staff) (current.keys + preparation.roster.keys).distinct()
                                  else preparation.roster.values.filter { it.invited }.map { it.playerId }
                        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            item { Text(if (staff) "Convoca jugadores de este equipo. El jugador o tutor puede responder; también puedes registrar su respuesta en su nombre."
                                else "Responde por tu ficha. Tu entrenador recibe el cambio sin recargar.") }
                            if (ids.isEmpty()) item { Text(if (staff) "No hay fichas disponibles. Revisa el plantel del equipo." else "Todavía no hay jugadores convocados.") }
                            items(ids, key = { it }) { id ->
                                val row = preparation.roster[id]
                                val player = current[id]
                                OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(row?.name ?: player?.name.orEmpty(), style = MaterialTheme.typography.titleMedium)
                                    Text(if (row?.invited != true) "Sin convocatoria" else when (row.response) {
                                        Availability.PENDING -> "Pendiente de respuesta"
                                        Availability.CONFIRMED -> "Confirmó disponibilidad"
                                        Availability.DECLINED -> "No puede asistir"
                                    })
                                    row?.let { invitation -> MatchPreparationPolicy.responseAttribution(invitation)?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall)
                                    } }
                                    if (staff && row?.invited == true) {
                                        Text("Registrar respuesta en nombre del jugador", style = MaterialTheme.typography.labelLarge)
                                        Button(onClick = { vm.respond(id, Availability.CONFIRMED, recordedByCoach = true) },
                                            enabled = canWrite) { Text("Registrar que asistirá") }
                                        OutlinedButton(onClick = { vm.respond(id, Availability.DECLINED, recordedByCoach = true) },
                                            enabled = canWrite) { Text("Registrar que no asistirá") }
                                    }
                                    if (staff) {
                                        val display = player ?: Player(id = id, teamId = selectedTeam, name = row?.name.orEmpty())
                                        OutlinedButton(onClick = { vm.invite(display, row?.invited != true) },
                                            enabled = canWrite && (row?.invited == true || player?.isActive == true)) {
                                            Text(if (row?.invited == true) "Retirar convocatoria" else "Convocar")
                                        }
                                    }
                                    if (id in ownIds && row?.invited == true) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { vm.respond(id, Availability.CONFIRMED) }, enabled = canWrite) { Text("Confirmo") }
                                        OutlinedButton(onClick = { vm.respond(id, Availability.DECLINED) }, enabled = canWrite) { Text("No puedo") }
                                    }
                                } }
                            }
                        }
                    } else lineupStates.SaveableStateProvider("${match.id}/${preparation.teamId}") {
                        LineupContent(preparation, staff, canWrite,
                            onDirtyChange = { dirty -> dirtyTeams = if (dirty) (dirtyTeams + preparation.teamId).distinct() else dirtyTeams - preparation.teamId },
                            save = { starters, subs, positions, revision, done -> vm.lineup(starters, subs, positions, revision, done) })
                    }
                }
            }
        }
    }
    if (discardConfirmation) AlertDialog(onDismissRequest = { discardConfirmation = false },
        title = { Text("¿Salir sin guardar la alineación?") },
        text = { Text("Hay selecciones o posiciones sin guardar. Puedes volver a editarlas; si sales, se descartará este borrador. No se modifica la alineación guardada.") },
        confirmButton = { TextButton(onClick = { discardConfirmation = false; onClose() }) { Text("Descartar borrador y salir") } },
        dismissButton = { TextButton(onClick = { discardConfirmation = false }) { Text("Seguir editando") } })
}

@Composable
internal fun LineupContent(preparation: MatchPreparation, staff: Boolean, canWrite: Boolean,
                          onDirtyChange: (Boolean) -> Unit,
                          save: (List<String>, List<String>, Map<String, String>, Long, () -> Unit) -> Unit) {
    var starters by rememberSaveable(preparation.teamId) { mutableStateOf(preparation.starters.toList()) }
    var subs by rememberSaveable(preparation.teamId) { mutableStateOf(preparation.substitutes.toList()) }
    var positions by rememberSaveable(preparation.teamId) { mutableStateOf(preparation.positions.toMap()) }
    var baseRevision by rememberSaveable(preparation.teamId) { mutableLongStateOf(preparation.revision) }
    var dirty by rememberSaveable(preparation.teamId) { mutableStateOf(false) }
    fun reload() { starters = preparation.starters; subs = preparation.substitutes; positions = preparation.positions; baseRevision = preparation.revision; dirty = false }
    LaunchedEffect(preparation.revision, dirty) { if (!dirty) reload() }
    LaunchedEffect(dirty) { onDirtyChange(dirty) }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(if (preparation.lineupCurrent) "Alineación guardada y vigente" else if (preparation.hasLineup)
                    "Cambió la convocatoria o una respuesta. El entrenador debe revisar la alineación." else "El entrenador todavía no guardó una alineación.")
            }
            if (staff) {
                item { Text("Elige entre 1 y 11 titulares y hasta 12 suplentes confirmados. Asigna una posición a cada uno.") }
                val candidates = (preparation.roster.values.filter { it.invited && it.response == Availability.CONFIRMED }
                    .map { it.playerId } + starters + subs).distinct()
                items(candidates, key = { it }) { id ->
                    val row = preparation.roster[id]
                    val eligible = row?.invited == true && row.response == Availability.CONFIRMED
                    OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(row?.name ?: "Ficha no disponible", style = MaterialTheme.typography.titleMedium)
                        if (!eligible) {
                            Text("Este jugador ya no está confirmado. Retíralo de la alineación antes de guardar.")
                            OutlinedButton(onClick = {
                                starters = starters - id; subs = subs - id; positions = positions - id; dirty = true
                            }, enabled = canWrite) { Text("Quitar de esta alineación") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(id in starters, {
                                starters = if (id in starters) starters - id else starters + id
                                subs = subs - id; positions = if (id in starters) positions + (id to (positions[id] ?: "DELANTERO")) else positions - id; dirty = true
                            }, enabled = canWrite && eligible && UiInteractionPolicy.canChooseSlot(id in starters, starters.size, 11), label = { Text("Titular") })
                            FilterChip(id in subs, {
                                subs = if (id in subs) subs - id else subs + id
                                starters = starters - id; positions = if (id in subs) positions + (id to (positions[id] ?: "DELANTERO")) else positions - id; dirty = true
                            }, enabled = canWrite && eligible && UiInteractionPolicy.canChooseSlot(id in subs, subs.size, 12), label = { Text("Suplente") })
                        }
                        if (id in starters || id in subs) {
                            var open by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(onClick = { open = true }, enabled = canWrite) { Text(UiInteractionPolicy.positionLabel(positions[id] ?: "DELANTERO")) }
                                DropdownMenu(expanded = open && canWrite, onDismissRequest = { open = false }) {
                                    MatchPreparationPolicy.positions.forEach { position -> DropdownMenuItem(text = { Text(UiInteractionPolicy.positionLabel(position)) },
                                        onClick = { positions = positions + (id to position); dirty = true; open = false }) }
                                }
                            }
                        }
                    } }
                }
            } else if (preparation.lineupCurrent) {
                item { Text("Titulares", style = MaterialTheme.typography.titleMedium) }
                items(preparation.starters, key = { "starter-$it" }) { id -> Text("${preparation.roster[id]?.name.orEmpty()} · ${UiInteractionPolicy.positionLabel(preparation.positions[id].orEmpty())}") }
                item { Text("Suplentes", style = MaterialTheme.typography.titleMedium) }
                items(preparation.substitutes, key = { "sub-$it" }) { id -> Text("${preparation.roster[id]?.name.orEmpty()} · ${UiInteractionPolicy.positionLabel(preparation.positions[id].orEmpty())}") }
            }
        }
        if (staff) Surface(shadowElevation = 4.dp, tonalElevation = 3.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val validation = runCatching { MatchPreparationPolicy.validateLineup(preparation, starters, subs, positions) }.exceptionOrNull()?.message
                val stale = baseRevision != preparation.revision
                Text("${starters.size}/11 titulares · ${subs.size}/12 suplentes", style = MaterialTheme.typography.titleMedium)
                if (stale) {
                    Text("Hay información nueva. Vuelve a cargar antes de guardar.", color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { reload() }, enabled = canWrite) { Text("Volver a cargar alineación") }
                }
                validation?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Button(onClick = { save(starters, subs, positions, baseRevision) { dirty = false } },
                    enabled = canWrite && !stale && validation == null, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Guardar alineación")
                }
            }
        }
    }
}
