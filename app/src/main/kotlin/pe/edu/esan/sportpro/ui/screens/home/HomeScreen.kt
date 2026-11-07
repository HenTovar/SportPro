package pe.edu.esan.sportpro.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import pe.edu.esan.sportpro.data.model.*
import pe.edu.esan.sportpro.data.repository.MatchRepository
import pe.edu.esan.sportpro.data.repository.Result
import pe.edu.esan.sportpro.di.DefaultAppContainer
import pe.edu.esan.sportpro.ui.common.*
import pe.edu.esan.sportpro.ui.navigation.*
import pe.edu.esan.sportpro.ui.theme.SportProTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(navController: NavHostController) {
    val session = LocalTeamSession.current
    val container = remember { DefaultAppContainer() }
    val repo = remember { MatchRepository(com.google.firebase.firestore.FirebaseFirestore.getInstance(), com.google.firebase.auth.FirebaseAuth.getInstance()) }
    var matches by remember { mutableStateOf<List<LiveMatch>>(emptyList()) }
    var trainings by remember { mutableStateOf<List<Training>>(emptyList()) }
    var matchError by remember { mutableStateOf<String?>(null) }
    var trainingError by remember { mutableStateOf<String?>(null) }
    var matchesLoading by remember { mutableStateOf(false) }
    var trainingsLoading by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    val teamId = session.selected?.id.orEmpty()
    LaunchedEffect(teamId, session.profile?.uid, revision) {
        matches = emptyList(); matchError = null; matchesLoading = teamId.isNotBlank()
        if (teamId.isNotBlank()) repo.matches(teamId).collect { r -> when (r) {
            is Result.Success -> { matches = r.data; matchError = null; matchesLoading = false }
            is Result.Error -> { matchError = r.message; matchesLoading = false }
            else -> Unit
        } }
    }
    LaunchedEffect(teamId, session.profile?.uid, revision) {
        trainings = emptyList(); trainingError = null; trainingsLoading = teamId.isNotBlank()
        if (teamId.isNotBlank()) container.trainingRepository.getTrainingsByTeam(teamId).collect { r -> when (r) {
            is Result.Success -> { trainings = r.data; trainingsLoading = false; trainingError = null }
            is Result.Error -> { trainingError = r.message; trainingsLoading = false }
            else -> Unit
        } }
    }
    HomeDashboard(session.profile, session.loading, session.error ?: matchError ?: trainingError,
        isCreator(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser),
        onNavigate = { navController.navigate(it) { launchSingleTop = true } },
        onLogout = {
            container.authRepository.logout()
            navController.navigate("login") { popUpTo(navController.graph.id) { inclusive = true }; launchSingleTop = true }
        }, selectedTeam = session.selected, matches = matches, trainings = trainings, onRefresh = { revision++ }, agendaLoading = matchesLoading || trainingsLoading, familyContent = { FamilyLinkControls(session) })
}

@Composable
fun HomeDashboard(profile: User?, loading: Boolean, error: String?, creator: Boolean = false,
    onNavigate: (String) -> Unit = {}, onLogout: () -> Unit = {}, selectedTeam: Team? = null,
    matches: List<LiveMatch> = emptyList(), trainings: List<Training> = emptyList(), onRefresh: () -> Unit = {}, agendaLoading: Boolean = false, familyContent: @Composable () -> Unit = {}) {
    val staff = RolePolicy.isStaff(profile?.role)
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(60_000); now = System.currentTimeMillis() } }
    val team = selectedTeam?.id.orEmpty()
    val live = StructurePolicy.live(matches)
    val nextMatch = StructurePolicy.nextMatch(matches, now)
    val training = StructurePolicy.nextTraining(trainings, now)
    val pending = nextMatch ?: matches.firstOrNull { it.status == "not_started" }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("SPORTPRO", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            StatusBadge(if (creator) "Creador" else RolePolicy.label(profile?.role), BadgeTone.Positive)
        }
        SportHero(if (staff) "TU ACADEMIA, CONECTADA" else "EL EQUIPO EMPIEZA CONTIGO",
            if (profile?.name.isNullOrBlank()) "Tu próxima jugada" else "Hola, ${profile?.name?.substringBefore(' ')}",
            if (staff) "Tu agenda y el control del equipo, sin rodeos." else "Tu asistencia, tus partidos y tu comunidad.")
        if (loading || agendaLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { MessageNotice(it, true) }
        if (profile != null && profile.isActive) {
            if (profile.role == UserRole.PADRE) familyContent()
            TeamSelector(onManage = { onNavigate("teams") })
            if (team.isBlank() && !loading) EmptyState(Icons.Default.Person, "Elige tu equipo",
                if (staff) "Crea o selecciona un equipo para ver su agenda." else if (profile.role == UserRole.PADRE) "Pide al entrenador un código e introdúcelo en Mis hijos." else "Comparte tu UID con el entrenador para vincular tu ficha.")
            if (team.isNotBlank()) {
                SectionHeading("Hoy en ${selectedTeam?.name.orEmpty()}", "Agenda del equipo seleccionado")
                TextButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null); Text("Actualizar agenda") }
                live?.let {
                    ActionTile("EN VIVO · ${it.name}", "Abrir marcador y cronología", Icons.Default.PlayArrow, { onNavigate(StructurePolicy.match(it.id)) })
                }
                AgendaCard("Próximo partido", nextMatch?.name ?: if (agendaLoading) "Cargando agenda…" else null, nextMatch?.date,
                    "No hay un partido futuro programado", { onNavigate(nextMatch?.let { StructurePolicy.match(it.id) } ?: "matches/${StructurePolicy.component(team)}") })
                AgendaCard(if (training?.status == TrainingStatus.EN_CURSO) "Entrenamiento en curso" else "Próximo entrenamiento", training?.name ?: if (agendaLoading) "Cargando agenda…" else null, training?.date,
                    "No hay entrenamientos próximos", { onNavigate(StructurePolicy.team(team, "trainings")) })
                SectionHeading("Accesos rápidos", if (staff) "Gestiona sin volver a buscar el equipo" else "Tu progreso en un toque")
                if (staff) {
                    ActionTile("Registrar asistencia", training?.name ?: "Elegir entrenamiento", Icons.Default.Check, {
                        onNavigate(training?.let { StructurePolicy.attendance(team, it.id) } ?: StructurePolicy.team(team, "trainings"))
                    })
                    ActionTile(if (live != null) "Registrar evento" else "Preparar partido", live?.name ?: pending?.name ?: "Ver partidos del equipo", Icons.Default.Add, {
                        onNavigate(live?.let { StructurePolicy.match(it.id, event = true) } ?: pending?.let { StructurePolicy.match(it.id) } ?: "matches/${StructurePolicy.component(team)}")
                    })
                } else {
                    StructurePolicy.ownAttendance(profile)?.let { destination ->
                        ActionTile(if (profile.role == UserRole.PADRE) "Asistencia de mi hijo" else "Mi asistencia", "Historial personal", Icons.Default.Check, { onNavigate(destination) })
                    }
                    ActionTile("Mis partidos", "Agenda y marcadores de mi equipo", Icons.Default.PlayArrow, { onNavigate("matches/${StructurePolicy.component(team)}") })
                }
                ActionTile("Muro del equipo", "Publicaciones y conversaciones", Icons.Default.Favorite, { onNavigate("community/${StructurePolicy.component(team)}") })
                ActionTile("Ficha del equipo", "Plantel · entrenamientos · partidos · muro", Icons.Default.Person, { onNavigate(StructurePolicy.team(team)) })
            }
            if (!staff && profile.role != UserRole.PADRE) SelectionContainer { Text("Código para tu entrenador: ${profile.uid}", style = MaterialTheme.typography.bodySmall) }
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { onNavigate("profile") }) { Text("Mi perfil") }
            TextButton(onClick = onLogout) { Text("Cerrar sesión") }
        }
    }
}

@Composable
private fun AgendaCard(title: String, name: String?, date: Long?, empty: String, onOpen: () -> Unit) {
    ElevatedCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(name?.ifBlank { title } ?: empty, style = MaterialTheme.typography.titleMedium)
            if (date != null && date > 0) Text(SimpleDateFormat("dd/MM HH:mm", Locale("es", "PE")).format(Date(date)))
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable private fun HomeCoachPreview() { SportProTheme { Surface { HomeDashboard(User(uid = "demo", name = "Alex", role = UserRole.ENTRENADOR), false, null) } } }
