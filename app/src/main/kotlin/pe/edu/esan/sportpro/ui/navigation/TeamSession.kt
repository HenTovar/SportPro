package pe.edu.esan.sportpro.ui.navigation

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import pe.edu.esan.sportpro.data.model.*
import pe.edu.esan.sportpro.data.repository.Result
import pe.edu.esan.sportpro.di.DefaultAppContainer

data class TeamSession(
    val profile: User? = null,
    val teams: List<Team> = emptyList(),
    val selected: Team? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val select: (String) -> Unit = {},
    val account: User? = profile,
    val child: FamilyChild? = null,
    val selectChild: (String) -> Unit = {},
    val refresh: () -> Unit = {}
)
val LocalTeamSession = staticCompositionLocalOf { TeamSession() }

/** Local preference only, separated per authenticated uid; no Firestore writes. */
@Composable
fun rememberTeamSession(refreshKey: String = ""): TeamSession {
    val context = LocalContext.current.applicationContext
    val auth = remember { FirebaseAuth.getInstance() }
    var uid by remember { mutableStateOf(auth.currentUser?.uid.orEmpty()) }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { uid = it.currentUser?.uid.orEmpty() }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }
    val container = remember { DefaultAppContainer() }
    val prefs = remember { context.getSharedPreferences("navigation", Context.MODE_PRIVATE) }
    var remembered by remember(uid) { mutableStateOf(if (uid.isBlank()) "" else prefs.getString(StructurePolicy.selectionKey(uid)!!, "").orEmpty()) }
    var childKey by remember(uid) { mutableStateOf(if (uid.isBlank()) "" else prefs.getString("last_child_$uid", "").orEmpty()) }
    var revision by remember { mutableIntStateOf(0) }
    var profile by remember(uid) { mutableStateOf<User?>(null) }
    var teams by remember(uid) { mutableStateOf<List<Team>>(emptyList()) }
    var loading by remember(uid) { mutableStateOf(true) }
    var error by remember(uid) { mutableStateOf<String?>(null) }
    LaunchedEffect(uid, refreshKey, revision) {
        loading = true; error = null; teams = emptyList(); profile = null
        if (uid.isBlank()) { loading = false; return@LaunchedEffect }
        when (val result = container.authRepository.getCurrentUser().first { it !is Result.Loading }) {
            is Result.Success -> profile = result.data?.takeIf { it.uid == uid }
            is Result.Error -> error = result.message
            else -> Unit
        }
        val user = profile
        if (user?.isActive == true) {
            if (RolePolicy.isStaff(user.role)) {
                when (val result = container.teamRepository.getAllTeams().first { it !is Result.Loading }) {
                    is Result.Success -> teams = result.data
                    is Result.Error -> error = result.message
                    else -> Unit
                }
            } else {
                val ids = (FamilyPolicy.children(user).map { it.teamId } + user.teamId).filter { it.isNotBlank() }.distinct()
                val loaded = mutableListOf<Team>()
                ids.forEach { id -> when (val result = container.teamRepository.getTeamById(id).first { it !is Result.Loading }) {
                    is Result.Success -> loaded += result.data
                    is Result.Error -> error = result.message
                    else -> Unit
                } }
                teams = loaded
            }
        }
        loading = false
    }
    val child = FamilyPolicy.selected(profile, childKey)
    val displayProfile = if (profile?.role == UserRole.PADRE && child != null)
        profile?.copy(teamId = child.teamId, playerId = child.playerId) else profile
    val selected = StructurePolicy.selectedTeam(displayProfile, remembered, teams)
    LaunchedEffect(uid, selected?.id) {
        if (uid.isNotBlank() && selected != null) prefs.edit().putString(StructurePolicy.selectionKey(uid)!!, selected.id).apply()
    }
    return TeamSession(displayProfile, teams, selected, loading, error, select = { id ->
        if (teams.any { it.id == id && it.isActive && RolePolicy.canReadTeam(profile, id) }) {
            remembered = id
            if (profile?.role == UserRole.PADRE) FamilyPolicy.children(profile).firstOrNull { it.teamId == id }?.let {
                childKey = it.key; prefs.edit().putString("last_child_$uid", it.key).apply()
            }
            if (uid.isNotBlank()) prefs.edit().putString(StructurePolicy.selectionKey(uid)!!, id).apply()
        }
    }, account = profile, child = child, selectChild = { key ->
        if (FamilyPolicy.children(profile).any { it.key == key }) {
            childKey = key
            prefs.edit().putString("last_child_$uid", key).apply()
        }
    }, refresh = { revision++ })
}

/** Keep route context aligned after the authenticated team list has loaded. */
@Composable
fun BindSelectedTeam(teamId: String) {
    val session = LocalTeamSession.current
    LaunchedEffect(teamId, session.profile?.uid, session.loading, session.teams.map { it.id }) {
        if (!session.loading && session.selected?.id != teamId) session.select(teamId)
    }
}
