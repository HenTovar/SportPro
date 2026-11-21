package pe.edu.esan.sportpro.ui.guided

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import pe.edu.esan.sportpro.data.model.RolePolicy
import pe.edu.esan.sportpro.data.model.User
import pe.edu.esan.sportpro.data.model.UserRole
import pe.edu.esan.sportpro.ui.navigation.FamilyCodeIssuer

internal fun canIssueFamilyCodeFromEditor(actor: User?, teamId: String, playerId: String,
    editorReady: Boolean, busy: Boolean): Boolean =
    actor?.role == UserRole.ADMIN && RolePolicy.canReadPlayer(actor, teamId, playerId) && editorReady && !busy

/** Reuses the existing issuer on the live editor; authorization remains in Firestore. */
@Composable
internal fun GuidedFamilyCodeSection(actor: User?, teamId: String, playerId: String,
    editorReady: Boolean, busy: Boolean,
    issuer: @Composable (String, String) -> Unit = { team, player -> FamilyCodeIssuer(team, player) }) {
    if (!canIssueFamilyCodeFromEditor(actor, teamId, playerId, editorReady, busy)) return
    HorizontalDivider()
    Text("Vincular padre o apoderado", style = MaterialTheme.typography.titleMedium)
    Text("Este código familiar lo introduce el padre en Inicio → Mis hijos. No es el código para unirse al equipo.")
    issuer(teamId, playerId)
}
