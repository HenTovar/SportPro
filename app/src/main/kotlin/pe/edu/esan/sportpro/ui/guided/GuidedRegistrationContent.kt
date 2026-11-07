package pe.edu.esan.sportpro.ui.guided

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/** Credentials stay in transient state and are never saved to a navigation argument. */
@Composable
fun GuidedRegistrationContent(busy: Boolean, message: String?,
    onRegister: (GuidedFormDraft, String, String) -> Unit, onLogin: () -> Unit) {
    var draft by remember { mutableStateOf(GuidedFormDraft()) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf(emptyMap<ProfileField, String>()) }
    var accountError by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Crea tu cuenta", style = MaterialTheme.typography.headlineMedium)
        Text("Elige tu perfil y completa tus datos. Podrás revisarlos en Perfil → Mis datos.")
        GuidedDetailsForm(draft, FormAccess.REGISTRATION, busy, errors) { draft = it; errors = emptyMap() }
        if (draft.role != null) {
            HorizontalDivider()
            Text("Acceso a tu cuenta", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(email, { email = it.take(254) }, label = { Text("Correo electrónico *") },
                enabled = !busy, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth().testTag("guided_email"))
            OutlinedTextField(password, { password = it }, label = { Text("Contraseña (mínimo 6 caracteres) *") },
                enabled = !busy, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().testTag("guided_password"))
        }
        accountError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        message?.let { Text(it, Modifier.testTag("guided_status")) }
        if (busy) CircularProgressIndicator()
        Button(enabled = !busy, onClick = {
            errors = draft.errors(FormAccess.REGISTRATION)
            accountError = when {
                !email.trim().matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) -> "Escribe un correo válido."
                password.length < 6 -> "La contraseña debe tener al menos 6 caracteres."
                else -> null
            }
            if (errors.isEmpty() && accountError == null) onRegister(draft, email.trim(), password)
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("guided_register")) { Text("Crear cuenta") }
        TextButton(enabled = !busy, onClick = onLogin) { Text("Ya tengo cuenta") }
    }
}
