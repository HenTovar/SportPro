package pe.edu.esan.sportpro.ui.guided

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pe.edu.esan.sportpro.data.model.PlayerPosition
import pe.edu.esan.sportpro.data.model.RolePolicy
import pe.edu.esan.sportpro.data.model.UserRole

/** Reusable production form; transport and persistence belong to the repository gateway. */
@Composable
fun GuidedDetailsForm(draft: GuidedFormDraft, access: FormAccess, busy: Boolean,
                      errors: Map<ProfileField, String>, onChange: (GuidedFormDraft) -> Unit) {
    fun enabled(field: ProfileField) = !busy && draft.canEdit(field, access)
    @Composable fun input(field: ProfileField, value: String, label: String, keyboard: KeyboardType = KeyboardType.Text,
                          change: (String) -> GuidedFormDraft) {
        OutlinedTextField(value = value, onValueChange = { onChange(change(it)) }, label = { Text(label) },
            enabled = !busy, readOnly = !draft.canEdit(field, access), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard), isError = field in errors,
            supportingText = errors[field]?.let { message -> { Text(message) } },
            modifier = Modifier.fillMaxWidth().testTag("guided_${field.name}"))
    }
    if (access == FormAccess.REGISTRATION) {
        Text("¿Cómo usarás SportPro?", style = MaterialTheme.typography.titleMedium)
        listOf(UserRole.JUGADOR, UserRole.PADRE, UserRole.ENTRENADOR).forEach { role ->
            FilterChip(selected = draft.role == role, onClick = { onChange(draft.selectRole(role)) }, enabled = !busy,
                label = { Text(if (role == UserRole.PADRE) "Padre / tutor" else RolePolicy.label(role)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("guided_role_${role.name}"))
        }
        errors[ProfileField.ROLE]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    } else Text("Datos de ${RolePolicy.label(draft.role)}", style = MaterialTheme.typography.titleMedium)
    if (draft.role == null) return
    Text("Todos los campos son obligatorios, excepto estatura y peso cuando se muestran.", style = MaterialTheme.typography.bodySmall)
    input(ProfileField.NAME, draft.name, "Nombre completo *") { draft.copy(name = it.take(120)) }
    input(ProfileField.PHONE, draft.phone, "Teléfono de contacto *", KeyboardType.Phone) { draft.copy(phone = it.take(30)) }
    when (draft.role) {
        UserRole.JUGADOR -> {
            Text("Si eres menor, usa de preferencia el teléfono de tu padre o tutor.", style = MaterialTheme.typography.bodySmall)
            input(ProfileField.BIRTH_DATE, draft.birthDate, "Fecha de nacimiento (aaaa-mm-dd) *") { draft.copy(birthDate = it.take(10)) }
            input(ProfileField.HEIGHT, draft.heightCm, "Estatura en cm (opcional)", KeyboardType.Decimal) { draft.copy(heightCm = it.take(12)) }
            input(ProfileField.WEIGHT, draft.weightKg, "Peso en kg (opcional)", KeyboardType.Decimal) { draft.copy(weightKg = it.take(12)) }
            if (enabled(ProfileField.POSITION)) {
                var expanded by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("guided_POSITION")) {
                    Text(draft.position?.let(::positionLabel) ?: "Seleccionar posición *")
                }
                DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                    PlayerPosition.values().forEach { position ->
                        DropdownMenuItem(text = { Text(positionLabel(position)) }, onClick = { onChange(draft.copy(position = position)); expanded = false })
                    }
                }
            } else Text("Posición: ${draft.position?.let(::positionLabel) ?: "Sin completar"}", Modifier.testTag("guided_POSITION"))
            errors[ProfileField.POSITION]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text("Pie hábil *", style = MaterialTheme.typography.labelLarge)
            FootChoice.values().forEach { foot ->
                FilterChip(selected = draft.foot == foot, onClick = { onChange(draft.copy(foot = foot)) },
                    enabled = enabled(ProfileField.FOOT), label = { Text(foot.label) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("guided_foot_${foot.name}"))
            }
            errors[ProfileField.FOOT]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (access == FormAccess.SELF) Text("Tu entrenador administra posición y pie hábil; puedes editar tus datos personales.")
        }
        UserRole.ENTRENADOR -> {
            input(ProfileField.SPECIALTY, draft.specialtyOrLicense, "Especialidad o licencia *") { draft.copy(specialtyOrLicense = it.take(200)) }
            input(ProfileField.EXPERIENCE, draft.yearsExperience, "Años de experiencia (puede ser 0) *", KeyboardType.Number) { draft.copy(yearsExperience = it.take(9)) }
            if (access == FormAccess.REGISTRATION) Text("Pendiente de aprobación: un administrador debe aprobar tu solicitud. No tendrás permisos de entrenador hasta entonces.")
        }
        else -> Unit
    }
}

fun positionLabel(position: PlayerPosition): String = position.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
