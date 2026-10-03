package pe.edu.esan.sportpro.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
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

        // Equipos
        composable("teams") {
            TeamsScreen(navController = navController)
        }
        composable("createTeam") {
            CreateTeamScreen(navController = navController)
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
