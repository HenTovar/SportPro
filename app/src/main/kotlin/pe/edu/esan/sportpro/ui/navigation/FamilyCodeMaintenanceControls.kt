package pe.edu.esan.sportpro.ui.navigation

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.repository.FamilyRepository

@Composable
fun FamilyCodeMaintenanceControls(teamId: String) {
    val repo=remember {FamilyRepository(FirebaseFirestore.getInstance(),FirebaseAuth.getInstance())};val scope=rememberCoroutineScope()
    var busy by remember {mutableStateOf(false)};var message by remember {mutableStateOf("")}
    var plan by remember {mutableStateOf<FamilyRepository.CleanupPlan?>(null)}
    Text("Máximo 20 códigos por hora y cuenta. Los códigos nuevos no guardan nombres de menores.")
    OutlinedButton(enabled=!busy,onClick={busy=true;scope.launch {
        try {val next=repo.previewCleanup(teamId)
            if(next.staleIds.isEmpty()&&next.legacyNameIds.isEmpty())message="No hay códigos usados/vencidos ni nombres heredados que limpiar en esta página (máximo 100)."
            else plan=next
        } catch(e:CancellationException){throw e} catch(_:Exception){message="No se pudo preparar la limpieza; revisa sesión y reglas."} finally{busy=false}
    }}) {Text("Revisar códigos para limpiar")}
    if(message.isNotBlank())Text(message)
    plan?.let {candidate->AlertDialog(onDismissRequest={plan=null},title={Text("Confirmar limpieza limitada")},
        text={Text("Eliminar "+candidate.staleIds.size+" códigos usados o vencidos y quitar el nombre de "+candidate.legacyNameIds.size+" códigos heredados activos. No borra fichas, vínculos ni códigos nuevos activos.")},
        confirmButton={TextButton(enabled=!busy,onClick={plan=null;busy=true;scope.launch {
            try{repo.cleanup(candidate);message="Limpieza confirmada. Revisa otra página si había más de 100 códigos."}
            catch(e:CancellationException){throw e}catch(_:Exception){message="No se confirmó la limpieza. No se afirma que se haya borrado nada."}finally{busy=false}
        }}){Text("Confirmar cantidades")}},dismissButton={TextButton(onClick={plan=null}){Text("Cancelar")}})}
}
