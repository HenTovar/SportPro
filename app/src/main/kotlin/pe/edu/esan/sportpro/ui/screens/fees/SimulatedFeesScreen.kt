package pe.edu.esan.sportpro.ui.screens.fees

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import pe.edu.esan.sportpro.data.model.SimulatedFeeStatus
import pe.edu.esan.sportpro.data.model.SimulatedFeesPolicy
import pe.edu.esan.sportpro.data.model.User
import pe.edu.esan.sportpro.data.repository.Result
import pe.edu.esan.sportpro.data.repository.SimulatedFeePlayerChoice
import pe.edu.esan.sportpro.data.repository.SimulatedFeeSnapshot
import pe.edu.esan.sportpro.data.repository.SimulatedFeesRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class FeesUiState(
    val players: List<SimulatedFeePlayerChoice> = emptyList(),
    val playerId: String = "",
    val period: String = SimulatedFeesPolicy.currentPeriod(),
    val playersLoading: Boolean = false,
    val recordLoading: Boolean = false,
    val snapshot: SimulatedFeeSnapshot? = null,
    val draftStatus: SimulatedFeeStatus? = null,
    val baseRevision: Long? = null,
    val dirty: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null
)

private class SimulatedFeesViewModel(
    private val teamId: String,
    private val user: User,
    private val repository: SimulatedFeesRepository
) : ViewModel() {
    private val mutable = MutableStateFlow(FeesUiState())
    val state = mutable.asStateFlow()
    private var playersJob: Job? = null
    private var recordJob: Job? = null
    private var saveJob: Job? = null

    fun load() {
        if (mutable.value.busy) return
        playersJob?.cancel()
        recordJob?.cancel()
        mutable.update { it.copy(playersLoading = true, recordLoading = false,
            snapshot = null, error = null, notice = null, dirty = false, baseRevision = null, draftStatus = null) }
        playersJob = viewModelScope.launch {
            try {
                val players = withTimeoutOrNull(20_000) { repository.loadPlayers(teamId, user) }
                    ?: throw IllegalStateException("No hay confirmación del servidor.")
                val selected = mutable.value.playerId.takeIf { id -> players.any { it.id == id } }
                    ?: players.firstOrNull()?.id.orEmpty()
                mutable.update { it.copy(players = players, playerId = selected, playersLoading = false) }
                if (selected.isNotBlank()) watch()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                mutable.update { it.copy(players = emptyList(), playerId = "", playersLoading = false,
                    error = repository.errorMessage(e)) }
            }
        }
    }

    private fun watch() {
        recordJob?.cancel()
        val selected = mutable.value
        mutable.update { it.copy(snapshot = null, recordLoading = true, error = null,
            draftStatus = null, baseRevision = null, dirty = false) }
        recordJob = viewModelScope.launch {
            repository.watch(teamId, selected.playerId, selected.period, user).collect { result ->
                when (result) {
                    is Result.Loading -> mutable.update { it.copy(recordLoading = true) }
                    is Result.Error -> mutable.update { it.copy(snapshot = null, recordLoading = false,
                        baseRevision = null, error = result.message) }
                    is Result.Success -> mutable.update { current ->
                        val snapshot = result.data
                        current.copy(snapshot = snapshot, recordLoading = false, error = null,
                            draftStatus = if (current.dirty) current.draftStatus else snapshot.record?.status,
                            baseRevision = if (current.dirty) current.baseRevision
                                else snapshot.takeIf { it.confirmed }?.revision)
                    }
                }
            }
        }
    }

    fun selectPlayer(playerId: String) {
        val current = mutable.value
        if (current.busy || current.playerId == playerId || current.players.none { it.id == playerId }) return
        mutable.update { it.copy(playerId = playerId, notice = null) }
        watch()
    }

    fun movePeriod(months: Int) {
        if (mutable.value.busy) return
        try {
            val period = SimulatedFeesPolicy.movePeriod(mutable.value.period, months)
            mutable.update { it.copy(period = period, notice = null) }
            if (mutable.value.playerId.isNotBlank()) watch()
        } catch (e: IllegalArgumentException) {
            mutable.update { it.copy(error = e.message) }
        }
    }

    fun reload() {
        if (mutable.value.busy) return
        mutable.update { it.copy(notice = null) }
        if (mutable.value.playerId.isBlank()) load() else watch()
    }

    fun choose(status: SimulatedFeeStatus) {
        val current = mutable.value
        if (!SimulatedFeesPolicy.canManage(user) || current.busy || current.snapshot?.confirmed != true) return
        mutable.update { it.copy(draftStatus = status, dirty = true, notice = null) }
    }

    fun save() {
        val selected = mutable.value
        val snapshot = selected.snapshot ?: return
        val status = selected.draftStatus ?: return
        val revision = selected.baseRevision ?: return
        if (selected.busy || !selected.dirty || !snapshot.confirmed || !SimulatedFeesPolicy.canManage(user)) return
        if (revision != snapshot.revision) {
            mutable.update { it.copy(error = "El registro cambió. Vuelve a cargar antes de guardar.") }
            return
        }
        mutable.update { it.copy(busy = true, error = null, notice = null) }
        saveJob = viewModelScope.launch {
            try {
                val acknowledged = withTimeoutOrNull(20_000) {
                    repository.save(teamId, selected.playerId, selected.period, status, revision, user)
                }
                if (acknowledged == null) {
                    mutable.update { it.copy(error = "No se recibió confirmación. Vuelve a cargar antes de reintentar.",
                        snapshot = null, baseRevision = null) }
                } else {
                    mutable.update { it.copy(notice = "Estado simulado confirmado por el servidor.") }
                    watch()
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                mutable.update { it.copy(error = repository.errorMessage(e), snapshot = null, baseRevision = null) }
            } finally { mutable.update { it.copy(busy = false) } }
        }
    }

    fun stop() {
        playersJob?.cancel()
        recordJob?.cancel()
        saveJob?.cancel()
    }
}

/** The caller supplies the current session profile; Firestore rules decide access. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatedFeesScreen(teamId: String, user: User, onBack: () -> Unit) {
    val vm: SimulatedFeesViewModel = viewModel(
        key = "simulated-fees-$teamId-${user.uid}-${user.hashCode()}",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SimulatedFeesViewModel(teamId, user,
                    SimulatedFeesRepository(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance())) as T
        }
    )
    val state by vm.state.collectAsState()
    val administrator = SimulatedFeesPolicy.canManage(user)
    val canReadTeam = SimulatedFeesPolicy.validId(teamId) && user.uid.isNotBlank() && user.isActive &&
        (administrator || pe.edu.esan.sportpro.data.model.FamilyPolicy.children(user).any { it.teamId == teamId })
    LaunchedEffect(vm, canReadTeam) { if (canReadTeam) vm.load() }
    DisposableEffect(vm) { onDispose { vm.stop() } }
    BackHandler(enabled = state.busy) { }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Mensualidades simuladas") }, navigationIcon = {
            TextButton(onClick = onBack, enabled = !state.busy) { Text("Volver") }
        })
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("Simulación académica", style = MaterialTheme.typography.titleMedium)
                Text("Estados de prueba. No se realizan cobros ni pagos desde esta pantalla.")
            }
            if (!canReadTeam) {
                item { Text("Esta sección está disponible para administradores y para la ficha autorizada del jugador o su apoderado.") }
            } else {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { vm.movePeriod(-1) }, enabled = !state.busy && state.period != "1000-01") { Text("Anterior") }
                        Text(state.period, style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { vm.movePeriod(1) }, enabled = !state.busy && state.period != "9999-12") { Text("Siguiente") }
                    }
                    Text("Período de la simulación", style = MaterialTheme.typography.bodySmall)
                }
                if (state.playersLoading || state.recordLoading || state.busy) {
                    item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                }
                state.error?.let { error ->
                    item {
                        Text(error, color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = vm::reload, enabled = !state.busy) { Text("Volver a cargar") }
                    }
                }
                state.notice?.let { notice -> item { Text(notice) } }
                if (!state.playersLoading && state.players.isEmpty() && state.error == null) {
                    item { Text(if (administrator) "No hay fichas registradas en este equipo." else "No hay fichas autorizadas disponibles.") }
                }
                if (state.players.isNotEmpty()) {
                    item {
                        var open by remember { mutableStateOf(false) }
                        val player = state.players.firstOrNull { it.id == state.playerId }
                        Text("Ficha", style = MaterialTheme.typography.labelLarge)
                        Box {
                            OutlinedButton(onClick = { open = true }, enabled = !state.busy,
                                modifier = Modifier.fillMaxWidth()) {
                                Text(player?.name.orEmpty(), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            DropdownMenu(expanded = open && !state.busy, onDismissRequest = { open = false }) {
                                state.players.forEach { choice ->
                                    DropdownMenuItem(text = { Text(choice.name) }, onClick = {
                                        open = false; vm.selectPlayer(choice.id)
                                    })
                                }
                            }
                        }
                    }
                }
                val snapshot = state.snapshot
                if (snapshot != null) {
                    item {
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(if (snapshot.confirmed) SimulatedFeesPolicy.label(snapshot.record?.status)
                                    else "Información sin confirmar", style = MaterialTheme.typography.titleLarge)
                                if (!snapshot.confirmed) {
                                    Text("La copia local puede estar desactualizada. Conéctate y vuelve a cargar para confirmar el estado.")
                                } else if (snapshot.record == null) {
                                    Text("No existe un registro para esta ficha y período. Esto no indica deuda ni pago.")
                                } else {
                                    Text("Registro de prueba del período ${state.period}.")
                                    snapshot.record.updatedAtMillis?.let { millis ->
                                        Text("Actualizado: " + SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE")).format(Date(millis)),
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                    if (administrator) {
                        item {
                            Text("Estado simulado", style = MaterialTheme.typography.titleMedium)
                            SimulatedFeeStatus.values().forEach { status ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = state.draftStatus == status,
                                        onClick = { vm.choose(status) }, enabled = snapshot.confirmed && !state.busy)
                                    Text(SimulatedFeesPolicy.label(status))
                                }
                            }
                            val stale = state.dirty && state.baseRevision != snapshot.revision
                            if (stale) {
                                Text("Hay un cambio de otro administrador. Vuelve a cargar antes de guardar.",
                                    color = MaterialTheme.colorScheme.error)
                            }
                            if (state.dirty) Text("Borrador sin guardar. Cambiar de ficha o período descarta este borrador.")
                            Button(onClick = vm::save,
                                enabled = snapshot.confirmed && !state.busy && state.dirty &&
                                    state.draftStatus != null && state.baseRevision != null && !stale) {
                                Text(if (state.busy) "Esperando confirmación…" else "Guardar estado simulado")
                            }
                            OutlinedButton(onClick = vm::reload, enabled = !state.busy) { Text("Volver a cargar") }
                        }
                    } else {
                        item { Text("Sólo un administrador puede cambiar estos estados simulados.") }
                    }
                } else if (state.playerId.isNotBlank() && !state.recordLoading && state.error == null) {
                    item { OutlinedButton(onClick = vm::reload, enabled = !state.busy) { Text("Cargar registro") } }
                }
            }
        }
    }
}
