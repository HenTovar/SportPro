package pe.edu.esan.sportpro.ui.screens.home

import android.net.Uri
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
import pe.edu.esan.sportpro.data.model.*
import pe.edu.esan.sportpro.data.repository.Result
import pe.edu.esan.sportpro.di.DefaultAppContainer
import pe.edu.esan.sportpro.ui.common.*
import pe.edu.esan.sportpro.ui.theme.SportProTheme

@Composable
fun HomeScreen(navController:NavHostController) {
    val container=remember {DefaultAppContainer()}
    var user by remember {mutableStateOf<User?>(null)}
    var loading by remember {mutableStateOf(true)}
    var error by remember {mutableStateOf<String?>(null)}
    LaunchedEffect(Unit) {container.authRepository.getCurrentUser().collect {r->when(r) {
        is Result.Success -> {user=r.data;loading=false}
        is Result.Error -> {error=r.message;loading=false}
        is Result.Loading -> loading=true
    }}}
    HomeDashboard(user,loading,error,isCreator(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser),
        onNavigate={navController.navigate(it)},onLogout={container.authRepository.logout();navController.navigate("login"){popUpTo("home"){inclusive=true}}})
}

@Composable
fun HomeDashboard(profile:User?,loading:Boolean,error:String?,creator:Boolean=false,onNavigate:(String)->Unit={},onLogout:()->Unit={}) {
    val staff=RolePolicy.isStaff(profile?.role)
    val assigned=profile!=null&&profile.teamId.isNotBlank()&&profile.playerId.isNotBlank()
    val team=Uri.encode(profile?.teamId.orEmpty())
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("SPORTPRO",style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary)
            StatusBadge(if(creator)"Creador" else RolePolicy.label(profile?.role),BadgeTone.Positive)
        }
        SportHero(if(staff)"TU ACADEMIA, CONECTADA" else "EL EQUIPO EMPIEZA CONTIGO",
            if(profile?.name.isNullOrBlank())"Tu próxima jugada" else "Hola, ${profile?.name?.substringBefore(' ')}",
            if(staff)"Organiza el día. Acompaña al equipo. Vive cada partido." else if(profile?.role==UserRole.PADRE)"Acompaña su progreso, dentro y fuera de la cancha." else "Tu progreso y tu equipo, en un mismo lugar.")
        if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let {MessageNotice(it,true)}
        if(profile!=null) {
            if(!staff&&!assigned)EmptyState(Icons.Default.Person,"Conectemos tu equipo","Comparte tu código con el entrenador para vincular tu ficha o la de tu hijo.")
            if(staff||assigned) {
                SectionHeading(if(staff)"Centro de entrenamiento" else "Tu día en el equipo","Elige dónde quieres continuar")
                if(!staff)ActionTile(if(profile.role==UserRole.PADRE)"Ficha de mi hijo" else "Mi ficha","Información de tu jugador",Icons.Default.Person,{onNavigate("players/$team")})
                ActionTile(if(staff)"Equipos" else "Mi equipo",if(staff)"Planteles y gestión de jugadores" else "Tu plantel y categoría",Icons.Default.Person,{onNavigate("teams")})
                ActionTile("Partidos","Marcador, eventos y cronología en vivo",Icons.Default.PlayArrow,{onNavigate(if(staff)"teams?pick=matches" else "matches/$team")})
                ActionTile("Comunidad","Las novedades de tu equipo",Icons.Default.Favorite,{onNavigate(if(staff)"teams?pick=community" else "community/$team")})
                ActionTile("Entrenamientos","Planificación, horarios y asistencia",Icons.Default.DateRange,{onNavigate(if(staff)"teams?pick=trainings" else "trainings/$team")})
                if(!staff)ActionTile("Mi asistencia","Calendario y seguimiento",Icons.Default.Check,{onNavigate("attendanceHistory/$team/${Uri.encode(profile.playerId)}")})
            }
            if(!staff)SelectionContainer {Text("Código para tu entrenador: ${profile.uid}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        }
        HorizontalDivider(Modifier.padding(vertical=8.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick={onNavigate("profile")},modifier=Modifier.weight(1f).heightIn(min=48.dp)){Icon(Icons.Default.Person,null);Spacer(Modifier.width(8.dp));Text("Mi perfil")}
            TextButton(onClick=onLogout,modifier=Modifier.heightIn(min=48.dp)){Text("Salir")}
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Preview(showBackground=true,widthDp=390,heightDp=900)
@Composable private fun HomeCoachPreview() {SportProTheme {Surface {HomeDashboard(User(uid="demo",name="Alex",role=UserRole.ENTRENADOR),false,null)}}}
@Preview(showBackground=true,widthDp=390,heightDp=900)
@Composable private fun HomeParentDarkPreview() {SportProTheme(darkTheme=true) {Surface {HomeDashboard(User(uid="demo",name="Alex",role=UserRole.PADRE,teamId="a",playerId="one"),false,null)}}}
