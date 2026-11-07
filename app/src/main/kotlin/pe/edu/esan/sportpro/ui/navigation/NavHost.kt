package pe.edu.esan.sportpro.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import pe.edu.esan.sportpro.ui.screens.teams.TeamScreen
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pe.edu.esan.sportpro.ui.screens.auth.LoginScreen
import pe.edu.esan.sportpro.ui.screens.auth.RegisterScreen
import pe.edu.esan.sportpro.ui.screens.home.HomeScreen
import pe.edu.esan.sportpro.ui.screens.teams.TeamsScreen
import pe.edu.esan.sportpro.ui.screens.teams.CreateTeamScreen
import pe.edu.esan.sportpro.ui.screens.players.PlayersScreen
import pe.edu.esan.sportpro.ui.screens.players.CreatePlayerScreen
import pe.edu.esan.sportpro.ui.screens.trainings.TrainingsScreen
import pe.edu.esan.sportpro.ui.screens.trainings.CreateTrainingScreen
import pe.edu.esan.sportpro.ui.screens.attendance.AttendanceScreen
import pe.edu.esan.sportpro.ui.screens.attendance.PlayerAttendanceHistoryScreen

/**
 * NavHost de SportPro.
 * Define todas las rutas de navegación de la aplicación.
 */
@Composable
fun SportProNavHost(
    startDestination: String = "login",
    navController: NavHostController = rememberNavController()
) {
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route.orEmpty()
    val protected = route.isNotBlank() && route != "login" && route != "register"
    val session = rememberTeamSession(if (protected) route else "")
    LaunchedEffect(protected, session.loading, session.profile?.uid) {
        if (protected && !session.loading && com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) {
            navController.navigate("login") { popUpTo(navController.graph.id) { inclusive = true }; launchSingleTop = true }
        }
    }
    CompositionLocalProvider(LocalTeamSession provides session) {
        AppContentFrame(showNavigation = protected, bottomBar = {
            MainNavigationBar(navController, route)
        }) {
            AppDestinations(startDestination, navController)
        }
    }
}

@Composable
private fun AppDestinations(startDestination: String, navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Autenticación
        composable("login") {
            LoginScreen(navController = navController)
        }
        composable("register") {
            RegisterScreen(navController = navController)
        }

        // Home
        composable("home") {
            HomeScreen(navController = navController)
        }

        composable("profile") {
            pe.edu.esan.sportpro.ui.screens.home.AdminProfileScreen(navController)
        }
        composable("adminRequests") {
            pe.edu.esan.sportpro.ui.screens.home.AdminRequestsScreen(navController)
        }
        // Equipos
        composable("teams?pick={pick}", arguments = listOf(navArgument("pick") { defaultValue = "" })) { entry ->
            TeamsScreen(
                navController = navController,
                pickTrainings = entry.arguments?.getString("pick") == "trainings",
                pickMatches = entry.arguments?.getString("pick") == "matches",
                pickCommunity = entry.arguments?.getString("pick") == "community"
            )
        }
        composable("createTeam") {
            CreateTeamScreen(navController = navController)
        }

        composable("editTeam/{teamId}") { entry ->
            CreateTeamScreen(navController = navController, teamId = entry.arguments?.getString("teamId") ?: "")
        }
        composable("editPlayer/{teamId}/{playerId}") { entry ->
            CreatePlayerScreen(teamId = entry.arguments?.getString("teamId") ?: "", navController = navController,
                playerId = entry.arguments?.getString("playerId") ?: "")
        }
        composable("editTraining/{teamId}/{trainingId}") { entry ->
            CreateTrainingScreen(teamId = entry.arguments?.getString("teamId") ?: "", navController = navController,
                trainingId = entry.arguments?.getString("trainingId") ?: "")
        }
        composable("matches/{teamId}") { entry ->
            pe.edu.esan.sportpro.ui.screens.home.MatchesScreen(entry.arguments?.getString("teamId") ?: "", navController)
        }
        composable("liveMatch/{matchId}?event={event}", arguments = listOf(navArgument("event") { type = NavType.BoolType; defaultValue = false })) { entry ->
            pe.edu.esan.sportpro.ui.screens.matches.LiveMatchScreen(entry.arguments?.getString("matchId") ?: "", navController,
                openEvent = entry.arguments?.getBoolean("event") == true)
        }
        composable("team/{teamId}?tab={tab}", arguments = listOf(navArgument("tab") { defaultValue = "players" })) { entry ->
            TeamScreen(entry.arguments?.getString("teamId").orEmpty(), entry.arguments?.getString("tab").orEmpty(), navController)
        }
        composable("community/{teamId}") { entry ->
            pe.edu.esan.sportpro.ui.screens.community.CommunityScreen(entry.arguments?.getString("teamId") ?: "", navController)
        }
        // Jugadores
        composable("players/{teamId}") { backStackEntry ->
            val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
            PlayersScreen(teamId = teamId, navController = navController)
        }
        composable("createPlayer/{teamId}") { backStackEntry ->
            val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
            CreatePlayerScreen(teamId = teamId, navController = navController)
        }

        // Entrenamientos
        composable("trainings/{teamId}") { backStackEntry ->
            val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
            TrainingsScreen(teamId = teamId, navController = navController)
        }
        composable("createTraining/{teamId}") { backStackEntry ->
            val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
            CreateTrainingScreen(teamId = teamId, navController = navController)
        }

        // Asistencia
        composable("attendance/{teamId}/{trainingId}") { backStackEntry ->
            val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
            val trainingId = backStackEntry.arguments?.getString("trainingId") ?: ""
            AttendanceScreen(teamId = teamId, trainingId = trainingId, navController = navController)
        }

        // Historial de asistencia de jugador
        composable("attendanceHistory/{teamId}/{playerId}") { backStackEntry ->
            val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
            val playerId = backStackEntry.arguments?.getString("playerId") ?: ""
            PlayerAttendanceHistoryScreen(teamId = teamId, playerId = playerId, navController = navController)
        }
    }
}

/**
 * Objetos de rutas para navegación tipada.
 */
object NavigationRoute {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val TEAMS = "teams"
    const val PICK_TEAM_FOR_TRAININGS = "teams?pick=trainings"
    const val CREATE_TEAM = "createTeam"
    const val PLAYERS = "players"
    const val CREATE_PLAYER = "createPlayer"
    const val TRAININGS = "trainings"
    const val CREATE_TRAINING = "createTraining"
    const val ATTENDANCE = "attendance"
    const val ATTENDANCE_HISTORY = "attendanceHistory"

    fun playersForTeam(teamId: String) = "players/$teamId"
    fun createPlayerForTeam(teamId: String) = "createPlayer/$teamId"
    fun trainingsForTeam(teamId: String) = "trainings/$teamId"
    fun createTrainingForTeam(teamId: String) = "createTraining/$teamId"
    fun attendanceForTraining(teamId: String, trainingId: String) = "attendance/$teamId/$trainingId"
    fun attendanceHistoryForPlayer(teamId: String, playerId: String) = "attendanceHistory/$teamId/$playerId"
}
