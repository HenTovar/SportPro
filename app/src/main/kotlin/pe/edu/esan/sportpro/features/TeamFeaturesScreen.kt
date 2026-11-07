package pe.edu.esan.sportpro.features

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import pe.edu.esan.sportpro.ui.common.BackButton
import pe.edu.esan.sportpro.ui.common.MessageNotice
import pe.edu.esan.sportpro.ui.navigation.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TeamFeaturesScreen(teamId: String,nav: NavHostController) {
    BindSelectedTeam(teamId)
    val state=LocalTeamFeatures.current
    val session=LocalTeamSession.current
    var selected by rememberSaveable(teamId) { mutableStateOf("calendar") }
    var year by rememberSaveable { mutableIntStateOf(Month.now().year) }
    var number by rememberSaveable { mutableIntStateOf(Month.now().number) }
    val month=Month(year,number)
    Scaffold(topBar={TopAppBar(title={Text("Agenda y progreso",style=MaterialTheme.typography.titleMedium)},navigationIcon={BackButton { nav.safeBack() }})}) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).testTag("team_features"),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item { Text(session.selected?.name?:"Equipo",style=MaterialTheme.typography.titleLarge) }
            item { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("calendar" to "Calendario","stats" to "Estadísticas").forEach { (key,label) ->
                    FilterChip(selected=selected==key,onClick={selected=key},label={Text(label)},modifier=Modifier.heightIn(min=48.dp))
                }
            } }
            if(state.team!=teamId || state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth());Text("Cargando datos autorizados…") }
            state.error?.let { item { MessageNotice(it,true) } }
            item { MonthChooser(month) {year=it.year;number=it.number} }
            if(state.team==teamId && state.error==null) {
                if(selected=="calendar") {
                    val entries=TeamInsights.agenda(state.matches,state.trainings).filter { month.contains(it.at) }
                    item { MonthCalendar(month,entries) { entry -> if(entry.kind.startsWith("Partido"))nav.navigate(entry.route) else nav.navigate(StructurePolicy.team(teamId,"trainings")) } }
                    if(entries.isEmpty())item { Text("No hay partidos ni entrenamientos registrados en este mes.") }
                    items(entries,key={it.id}){entry ->
                        OutlinedCard(onClick={if(entry.kind.startsWith("Partido"))nav.navigate(entry.route)else nav.navigate(StructurePolicy.team(teamId,"trainings"))},modifier=Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)){Text(entry.title,style=MaterialTheme.typography.titleMedium);Text(SimpleDateFormat("dd/MM HH:mm",Locale("es","PE")).format(Date(entry.at)));Text(entry.kind)}
                        }
                    }
                    item { val missing=state.matches.count { it.date<=0 }+state.trainings.count { it.date<=0 };if(missing>0)Text("$missing registros sin fecha válida no pueden ubicarse en el calendario.") }
                } else {
                    val goals=TeamInsights.goals(teamId,state.events)
                    val attendance=TeamInsights.attendance(month,state.attendance,state.trainings)
                    val streak=TeamInsights.streak(teamId,state.matches,state.events)
                    item { Text("Racha: "+if(streak.count>0)"${streak.count} ${streak.result}" else streak.result,style=MaterialTheme.typography.titleMedium) }
                    item { Text("Goles por jugador · todos los partidos cargados",style=MaterialTheme.typography.titleMedium);Text("Sólo goles vigentes de este equipo. Agrupados por el nombre escrito en el evento; los homónimos no se distinguen.",style=MaterialTheme.typography.bodySmall) }
                    if(goals.isEmpty())item { Text("Sin goles registrados") }
                    items(goals,key={it.first}) { row -> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text(row.first,Modifier.weight(1f));Text(row.second.toString())} }
                    item { Text("Asistencia del mes",style=MaterialTheme.typography.titleMedium)
                        Text("${attendance.present} presentes · ${attendance.absent} faltas · ${attendance.other} lesión/justificada")
                        Text(attendance.percent?.let { "$it% sobre ${attendance.total} registros" }?:"Aún no hay registros de este mes")
                        Text("Se usa la fecha del entrenamiento y el último registro por jugador/sesión. No se inventan faltas en sesiones sin registro. Padre/jugador ve sólo su ficha seleccionada.",style=MaterialTheme.typography.bodySmall)
                        if(attendance.unregistered>0)Text("${attendance.unregistered} registros sin entrenamiento fechado: fuera del cálculo.")
                    }
                    if(state.cached || state.pending || state.loading)item { Text("Cifras provisionales: datos incompletos, en caché o pendientes.",color=MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}
@Composable
fun MonthChooser(month: Month,onMonth:(Month)->Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(SimpleDateFormat("MMMM yyyy",Locale("es","PE")).format(month.calendar().time),style=MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            TextButton(enabled=month.year>1970 || month.number>1,onClick={onMonth(month.shift(-1))}){Text("Anterior")}
            TextButton(onClick={onMonth(Month.now())}){Text("Hoy")}
            TextButton(enabled=month.year<9999 || month.number<12,onClick={onMonth(month.shift(1))}){Text("Siguiente")}
        }
    }
}
@Composable
fun MonthCalendar(month: Month,entries: List<AgendaEntry>,onOpen:(AgendaEntry)->Unit) {
    var day by remember(month) { mutableIntStateOf(0) }
    val byDay=entries.groupBy { Calendar.getInstance().apply {timeInMillis=it.at}.get(Calendar.DAY_OF_MONTH) }
    Column(Modifier.fillMaxWidth().testTag("monthly_calendar")) {
        Row(Modifier.fillMaxWidth()) { listOf("L","M","X","J","V","S","D").forEach { Text(it,Modifier.weight(1f),style=MaterialTheme.typography.labelSmall) } }
        val offset=month.leading();val days=month.days()
        repeat((offset+days+6)/7) { week -> Row(Modifier.fillMaxWidth()) {
            repeat(7) { weekday -> val current=week*7+weekday-offset+1
                if(current !in 1..days)Spacer(Modifier.weight(1f)) else TextButton(onClick={day=current},modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("calendar_day_$current"),contentPadding=PaddingValues(0.dp)) {
                    Text(current.toString()+if(byDay[current].isNullOrEmpty())"" else "•",maxLines=1)
                }
            }
        } }
        Text("• indica actividad. Toca un día para ver sus registros.",style=MaterialTheme.typography.bodySmall)
        if(day>0) {Text("Día $day",style=MaterialTheme.typography.titleMedium)
            if(byDay[day].isNullOrEmpty())Text("Sin actividad registrada")
            byDay[day].orEmpty().forEach { entry -> TextButton(onClick={onOpen(entry)},modifier=Modifier.fillMaxWidth()){Text(entry.title+" · "+entry.kind)} }
        }
    }
}
