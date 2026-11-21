package pe.edu.esan.sportpro.ui.screens.matches

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import pe.edu.esan.sportpro.data.model.*

/** Actual lineup component with offline fixtures; no account, server or navigation claim. */
class LineupControlsUiTest {
    @get:Rule val rule = createComposeRule()
    private val ids=(1..23).map { "p$it" }
    private val roster=ids.associateWith { MatchInvitation(it,"Jugador $it",response=Availability.CONFIRMED) }
    @Test fun saveIsVisibleWithoutScrollingThroughTwentyThreePlayers() {
        val preparation=MatchPreparation(teamId="team",revision=7,lineupRevision=7,roster=roster,
            starters=ids.take(11),substitutes=ids.drop(11),positions=ids.associateWith { "DELANTERO" })
        var saved=0
        rule.setContent { MaterialTheme { Box(Modifier.width(390.dp).height(600.dp)) {
            LineupContent(preparation,true,true,{}, { starters,subs,_,revision,done ->
                assertEquals(11,starters.size);assertEquals(12,subs.size);assertEquals(7L,revision);saved++;done()
            })
        } } }
        rule.onNodeWithText("Guardar alineación").assertIsDisplayed().assertIsEnabled().performClick()
        rule.runOnIdle { assertEquals(1,saved) }
    }
    @Test fun tabRoundTripPreservesAnUnsavedSelection() {
        val show=mutableStateOf(true)
        val preparation=MatchPreparation(teamId="team",revision=7,roster=roster)
        rule.setContent { MaterialTheme {
            val holder=rememberSaveableStateHolder()
            Column(Modifier.width(390.dp).height(600.dp)) {
                TextButton(onClick={show.value=!show.value}) { Text(if(show.value)"Ver convocatoria" else "Volver a alineación") }
                Box(Modifier.weight(1f)) {
                    if(show.value)holder.SaveableStateProvider("lineup-team") {
                        LineupContent(preparation,true,true,{}, {_,_,_,_,_->})
                    } else Text("Convocatoria de prueba")
                }
            }
        } }
        rule.onAllNodesWithText("Titular").onFirst().performClick()
        rule.onNodeWithText("1/11 titulares · 0/12 suplentes").assertIsDisplayed()
        rule.onNodeWithText("Ver convocatoria").performClick()
        rule.onNodeWithText("Volver a alineación").performClick()
        rule.onNodeWithText("1/11 titulares · 0/12 suplentes").assertIsDisplayed()
    }
    @Test fun readOnlyLineupDoesNotOfferSave() {
        val preparation=MatchPreparation(teamId="team",revision=7,lineupRevision=7,roster=roster,
            starters=ids.take(1),positions=mapOf(ids.first() to "DELANTERO"))
        rule.setContent { MaterialTheme { Box(Modifier.width(390.dp).height(600.dp)) {
            LineupContent(preparation,false,false,{}, {_,_,_,_,_->fail("Read only")})
        } } }
        rule.onNodeWithText("Guardar alineación").assertDoesNotExist()
        rule.onNodeWithText("Jugador p1 · Delantero").assertIsDisplayed()
    }
}
