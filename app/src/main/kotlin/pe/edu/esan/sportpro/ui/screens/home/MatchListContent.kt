package pe.edu.esan.sportpro.ui.screens.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import pe.edu.esan.sportpro.data.model.LiveMatch
import pe.edu.esan.sportpro.ui.common.*
import pe.edu.esan.sportpro.ui.navigation.StructurePolicy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MatchStatusFilters(status: String, onStatus: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).heightIn(min = 48.dp)
        .testTag("match_filters"), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("" to "Todos", "not_started" to "Próximos", "in_progress" to "En vivo", "finished" to "Finalizados").forEach { (value, label) ->
            FilterChip(selected = status == value, onClick = { onStatus(value) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("match_filter_" + value),
                label = { Text(label, maxLines = 1, softWrap = false) })
        }
    }
}

/** One vertical scroller: the empty state can never squeeze the controls to zero height. */
@Composable
fun MatchListContent(rows: List<LiveMatch>, loading: Boolean, error: String?, staff: Boolean,
    embedded: Boolean, query: String, status: String, onQuery: (String) -> Unit,
    onStatus: (String) -> Unit, onCreate: () -> Unit, onOpen: (String) -> Unit,
    modifier: Modifier = Modifier) {
    val filtered = StructurePolicy.matches(rows, query, status.ifBlank { null })
    LazyColumn(modifier.fillMaxSize().testTag("match_list"), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!embedded) item(key = "hero") { SportHero("EL FÚTBOL SE VIVE AQUÍ", "Día de partido", "Sigue cada jugada. Comparte cada momento.") }
        if (staff) item(key = "create") { Button(onClick = onCreate, modifier = Modifier.heightIn(min = 48.dp).testTag("create_match")) { Text("Crear partido") } }
        item(key = "search") { MatchSearchField(query, onQuery) }
        item(key = "filters") { MatchStatusFilters(status, onStatus) }
        if (loading) item(key = "loading") { CircularProgressIndicator() }
        error?.let { item(key = "error") { MessageNotice(it, true) } }
        if (!loading && error == null && rows.isEmpty()) item(key = "empty") {
            EmptyState(Icons.Default.PlayArrow, "Tu próximo encuentro está por llegar", "El entrenador puede crear el primer partido con un rival registrado.")
        }
        if (rows.isNotEmpty() && filtered.isEmpty()) item(key = "no_results") { Text("No hay partidos con esos filtros") }
        items(filtered, key = { "match_" + it.id }) { row ->
            ElevatedCard(Modifier.fillMaxWidth(), elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(row.name, style = MaterialTheme.typography.titleMedium)
                    Text(if (row.date > 0) SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(row.date)) else "Fecha pendiente")
                    StatusBadge(when (row.status) { "finished" -> "Finalizado"; "in_progress" -> "En curso"; else -> "Programado" }, if (row.status == "in_progress") BadgeTone.Positive else BadgeTone.Neutral)
                    Button(onClick = { onOpen(row.id) }) { Text("Abrir en vivo") }
                }
            }
        }
    }
}
