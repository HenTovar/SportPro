package pe.edu.esan.sportpro.ui.screens.home

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import pe.edu.esan.sportpro.ui.navigation.StructurePolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import pe.edu.esan.sportpro.ui.common.*
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
import pe.edu.esan.sportpro.ui.navigation.BindSelectedTeam
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MatchesScreen(teamId:String,navController:NavHostController,embedded:Boolean=false) {
    BindSelectedTeam(teamId)
    val container=remember {DefaultAppContainer()};val repo=remember {MatchRepository(FirebaseFirestore.getInstance(),FirebaseAuth.getInstance())}
    var rows by remember {mutableStateOf<List<LiveMatch>>(emptyList())};var teams by remember {mutableStateOf<List<Team>>(emptyList())}
    var loading by remember {mutableStateOf(true)};var error by remember {mutableStateOf<String?>(null)}
    var staff by remember {mutableStateOf(false)};var create by remember {mutableStateOf(false)}
    var name by remember {mutableStateOf("")};var opponent by remember {mutableStateOf("")};var saving by remember {mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    var query by rememberSaveable(teamId) {mutableStateOf("")}
    var status by rememberSaveable(teamId) {mutableStateOf("")}
    LaunchedEffect(teamId) {
        val profile=container.authRepository.getCurrentUser().first {it !is Result.Loading}
        val user=(profile as? Result.Success)?.data
        if(!RolePolicy.canReadTeam(user,teamId)) {error="No tienes acceso a este equipo";loading=false;return@LaunchedEffect}
        staff=RolePolicy.isStaff(user?.role)
        repo.matches(teamId).collect {r->when(r) {
            is Result.Success -> {rows=r.data;loading=false}
            is Result.Error -> {error=r.message;loading=false}
            else -> Unit
        }}
    }
    LaunchedEffect(staff,teamId) {
        teams=emptyList()
        if(staff)container.teamRepository.getAllTeams().collect {if(it is Result.Success)teams=it.data.filter {t->t.isActive&&t.id!=teamId}}
    }
    Scaffold(topBar={if(!embedded)TopAppBar(title={Text("Partidos del equipo")},navigationIcon={pe.edu.esan.sportpro.ui.navigation.PrimaryDestinationBackButton(navController)})}) { padding ->
        MatchListContent(rows, loading, error, staff, embedded, query, status,
            onQuery = { query = it }, onStatus = { status = it }, onCreate = { create = true },
            onOpen = { navController.navigate("liveMatch/" + Uri.encode(it)) },
            modifier = Modifier.padding(padding).consumeWindowInsets(padding))
    }

    if(create)AlertDialog(onDismissRequest={if(!saving)create=false},title={Text("Nuevo partido")},text={Column {
        OutlinedTextField(name,{name=it},label={Text("Nombre del partido")})
        Text("Elige rival entre los equipos registrados")
        if(teams.isEmpty())Text("Registra otro equipo antes de crear un partido.")
        teams.forEach {t->Row {RadioButton(opponent==t.id,{opponent=t.id});Text(t.name)}}
        if(saving)CircularProgressIndicator()
        error?.let {MessageNotice(it,true)}
    }},confirmButton={TextButton(onClick={scope.launch {
        saving=true
        try {repo.create(teamId,opponent,name);create=false;name="";opponent="";error=null}
        catch(e:Exception){if(e is kotlinx.coroutines.CancellationException)throw e;error=e.message ?: "No se pudo crear"}
        finally{saving=false}
    }},enabled=!saving&&name.isNotBlank()&&name.length<=100&&opponent.isNotBlank()){Text("Crear")}},dismissButton={TextButton(onClick={create=false},enabled=!saving){Text("Cancelar")}})
}
