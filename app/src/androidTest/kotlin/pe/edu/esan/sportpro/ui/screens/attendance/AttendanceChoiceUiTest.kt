package pe.edu.esan.sportpro.ui.screens.attendance

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import pe.edu.esan.sportpro.data.model.Player

/** Offline component cases. No Firebase, storage or repository calls. */
class AttendanceChoiceUiTest {
    @get:Rule val rule = createComposeRule()
    @Test fun pressingSelectedPresentDoesNotInvertTheMark() {
        val present=mutableStateOf(true);var writes=0
        rule.setContent { MaterialTheme { AttendancePlayerItem(Player(id="p",name="Jugador de prueba"),
            present.value,{present.value=it;writes++},true,hasChoice=true) } }
        rule.onNodeWithText("Presente").performClick()
        rule.runOnIdle { assertTrue(present.value);assertEquals(0,writes) }
        rule.onNodeWithText("Ausente").performClick()
        rule.runOnIdle { assertFalse(present.value);assertEquals(1,writes) }
        rule.onNodeWithText("Ausente").performClick()
        rule.runOnIdle { assertEquals(1,writes) }
    }
    @Test fun absenceCanBeExplicitlyRegisteredFromUnknown() {
        val chosen=mutableStateOf(false);var value:Boolean?=null
        rule.setContent { MaterialTheme { AttendancePlayerItem(Player(id="p",name="Jugador de prueba"),
            false,{value=it;chosen.value=true},true,statusLabel="Sin registrar",hasChoice=chosen.value) } }
        rule.onNodeWithText("Ausente").performClick()
        rule.runOnIdle { assertEquals(false,value) }
    }
    @Test fun readOnlyRowHasNoMutationAction() {
        var writes=0
        rule.setContent { MaterialTheme { AttendancePlayerItem(Player(id="p",name="Jugador de prueba"),true,{writes++},false) } }
        rule.onAllNodes(hasClickAction()).assertCountEquals(0)
        rule.runOnIdle { assertEquals(0,writes) }
    }
}
