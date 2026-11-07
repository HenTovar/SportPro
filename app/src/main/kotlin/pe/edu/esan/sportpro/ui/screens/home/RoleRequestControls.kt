package pe.edu.esan.sportpro.ui.screens.home

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import pe.edu.esan.sportpro.data.model.RolePolicy
import pe.edu.esan.sportpro.data.model.UserRole

@Composable
fun ProfileRoleSummary(role: UserRole?, loading: Boolean, loadError: Boolean, creator: Boolean) {
    Text(ProfileRoleUi.roleText(role, loading, loadError, creator), Modifier.testTag("profile_actual_role"))
}

@Composable
fun RoleRequestControls(role: UserRole?, target: UserRole?, loading: Boolean, loadError: Boolean,
                        busy: Boolean, requested: Boolean, onTarget: (UserRole) -> Unit, onRequest: () -> Unit) {
    Text("Solicitar cambio de rol", style = MaterialTheme.typography.titleMedium)
    UserRole.values().filter { it != role }.forEach { candidate ->
        Row {
            RadioButton(selected = target == candidate, onClick = { onTarget(candidate) },
                enabled = role != null && !loading && !loadError && !busy && !requested,
                modifier = Modifier.testTag("role_request_${candidate.name}"))
            Text(RolePolicy.label(candidate))
        }
    }
    Text("Elige el rol que deseas solicitar. Esto no es tu rol actual y no cambia tus permisos.")
    Text("Una persona autorizada debe aprobar la solicitud.")
    Button(enabled = ProfileRoleUi.canRequest(role, target, loading, loadError, busy, requested),
        onClick = onRequest, modifier = Modifier.testTag("role_request_submit")) {
        Text("Enviar solicitud")
    }
}
