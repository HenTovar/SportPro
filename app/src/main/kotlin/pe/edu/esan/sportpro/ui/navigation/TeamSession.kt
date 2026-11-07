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
    val select: (String) -> Unit = {}
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
    var profile by remember(uid) { mutableStateOf<User?>(null) }
    var teams by remember(uid) { mutableStateOf<List<Team>>(emptyList()) }
    var loading by remember(uid) { mutableStateOf(true) }
    var error by remember(uid) { mutableStateOf<String?>(null) }
    LaunchedEffect(uid, refreshKey) {
        loading = true; error = null; teams = emptyList()
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
            } else if (user.teamId.isNotBlank()) {
                when (val result = container.teamRepository.getTeamById(user.teamId).first { it !is Result.Loading }) {
                    is Result.Success -> teams = listOf(result.data)
                    is Result.Error -> error = result.message
                    else -> Unit
                }
            }
        }
        loading = false
    }
    val selected = StructurePolicy.selectedTeam(profile, remembered, teams)
    LaunchedEffect(uid, selected?.id) {
        if (uid.isNotBlank() && selected != null) prefs.edit().putString(StructurePolicy.selectionKey(uid)!!, selected.id).apply()
    }
    return TeamSession(profile, teams, selected, loading, error) { id ->
        if (teams.any { it.id == id && it.isActive && RolePolicy.canReadTeam(profile, id) }) {
            remembered = id
            if (uid.isNotBlank()) prefs.edit().putString(StructurePolicy.selectionKey(uid)!!, id).apply()
        }
    }
}

/** Keep route context aligned after the authenticated team list has loaded. */
@Composable
fun BindSelectedTeam(teamId: String) {
    val session = LocalTeamSession.current
    LaunchedEffect(teamId, session.profile?.uid, session.loading, session.teams.map { it.id }) {
        if (!session.loading && session.selected?.id != teamId) session.select(teamId)
    }
}
