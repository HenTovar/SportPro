package pe.edu.esan.sportpro.ui.screens.home

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.*
import pe.edu.esan.sportpro.data.repository.MatchRepository
import pe.edu.esan.sportpro.data.repository.Result
import pe.edu.esan.sportpro.di.DefaultAppContainer
import pe.edu.esan.sportpro.ui.navigation.safeBack
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MatchesScreen(teamId:String,navController:NavHostController) {
    val container=remember {DefaultAppContainer()};val repo=remember {MatchRepository(FirebaseFirestore.getInstance(),FirebaseAuth.getInstance())}
    var rows by remember {mutableStateOf<List<LiveMatch>>(emptyList())};var teams by remember {mutableStateOf<List<Team>>(emptyList())}
    var loading by remember {mutableStateOf(true)};var error by remember {mutableStateOf<String?>(null)}
    var staff by remember {mutableStateOf(false)};var create by remember {mutableStateOf(false)}
    var name by remember {mutableStateOf("")};var opponent by remember {mutableStateOf("")};var saving by remember {mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    LaunchedEffect(teamId) {
        val profile=container.authRepository.getCurrentUser().first {it !is Result.Loading}
        val user=(profile as? Result.Success)?.data
        if(!RolePolicy.canReadTeam(user,teamId)) {error="No tienes acceso a este equipo";loading=false;return@LaunchedEffect}
        staff=RolePolicy.isStaff(user?.role)
        if(staff)container.teamRepository.getAllTeams().collect {if(it is Result.Success)teams=it.data.filter {t->t.isActive&&t.id!=teamId}}
        repo.matches(teamId).collect {r->when(r) {
            is Result.Success -> {rows=r.data;loading=false}
            is Result.Error -> {error=r.message;loading=false}
            else -> Unit
        }}
    }
    Scaffold(topBar={TopAppBar(title={Text("Partidos del equipo")},navigationIcon={TextButton(onClick={navController.safeBack()}){Text("Volver")}})}) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            if(staff)Button(onClick={create=true}){Text("Crear partido")}
            if(loading)CircularProgressIndicator()
            error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
            if(!loading&&error==null&&rows.isEmpty())Text("No hay partidos. El entrenador puede crear el primero.")
            LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {items(rows,key={it.id}) {row->Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp)) {
                Text(row.name,style=MaterialTheme.typography.titleMedium)
                Text(if(row.date>0)SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(Date(row.date)) else "Fecha pendiente")
                Text(when(row.status){"finished"->"Finalizado";"in_progress"->"En curso";else->"Programado"})
                Button(onClick={navController.navigate("liveMatch/${Uri.encode(row.id)}")}){Text("Abrir en vivo")}
            }}}}
        }
    }
    if(create)AlertDialog(onDismissRequest={if(!saving)create=false},title={Text("Nuevo partido")},text={Column {
        OutlinedTextField(name,{name=it},label={Text("Nombre del partido")})
        Text("Elige rival entre los equipos registrados")
        if(teams.isEmpty())Text("Registra otro equipo antes de crear un partido.")
        teams.forEach {t->Row {RadioButton(opponent==t.id,{opponent=t.id});Text(t.name)}}
        if(saving)CircularProgressIndicator()
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={TextButton(onClick={scope.launch {
        saving=true
        try {repo.create(teamId,opponent,name);create=false;name="";opponent="";error=null}
        catch(e:Exception){if(e is kotlinx.coroutines.CancellationException)throw e;error=e.message ?: "No se pudo crear"}
        finally{saving=false}
    }},enabled=!saving&&name.isNotBlank()&&name.length<=100&&opponent.isNotBlank()){Text("Crear")}},dismissButton={TextButton(onClick={create=false},enabled=!saving){Text("Cancelar")}})
}
