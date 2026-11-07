package pe.edu.esan.sportpro.ui.guided

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.RolePolicy
import pe.edu.esan.sportpro.data.model.TeamJoiner
import pe.edu.esan.sportpro.data.model.UserRole
import pe.edu.esan.sportpro.data.repository.Result
import pe.edu.esan.sportpro.di.DefaultAppContainer
import pe.edu.esan.sportpro.ui.navigation.LocalTeamSession
import pe.edu.esan.sportpro.ui.navigation.safeBack

@Composable
fun TeamCodeScreen(teamId: String, nav: NavHostController) {
    val session = LocalTeamSession.current
    val actor = session.account
    val scope = rememberCoroutineScope()
    val repo = remember { DefaultAppContainer().guidedAccounts }
    var code by remember(teamId, actor?.uid) { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var joiners by remember(teamId, actor?.uid) { mutableStateOf(emptyList<TeamJoiner>()) }
    var revision by remember { mutableIntStateOf(0) }
    var revokeConfirm by remember { mutableStateOf(false) }
    val allowed = !session.loading && session.error == null && RolePolicy.isStaff(actor?.role) && RolePolicy.canReadTeam(actor, teamId)
    LaunchedEffect(teamId, actor?.uid, allowed, revision) {
        joiners = emptyList()
        if (allowed) try { joiners = repo.loadTeamJoiners(teamId) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { message = "No se pudo cargar el registro de incorporaciones." }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Código de equipo", style = MaterialTheme.typography.headlineMedium)
        Text("El jugador completa sus datos y canjea el código en Perfil → Mis datos → Unirme a un equipo. No elijas una ficha ni escribas el UID de otra persona.")
        if (session.loading || busy) CircularProgressIndicator()
        if (!session.loading && !allowed) Text("Sólo el personal autorizado de este equipo puede generar el código.")
        Text("El código dura 72 horas o hasta 40 incorporaciones. Generar otro revoca el anterior. Máximo 20 emisiones por hora por persona autorizada.")
        TeamCodeDisplay(code = code, allowed = allowed, busy = busy,
            onCopied = { message = "Código de equipo copiado." })
        message?.let { Text(it) }
        Button(enabled = allowed && !busy, onClick = {
            busy = true; message = null; code = null
            scope.launch {
                try { code = repo.generateTeamCode(teamId); revision++ }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { message = "No se pudo generar el código. Revisa el acceso, la cuota o vuelve a intentarlo más tarde." }
                finally { busy = false }
            }
        }) { Text("Generar / renovar código") }
        OutlinedButton(enabled = allowed && !busy, onClick = { revokeConfirm = true }) { Text("Revocar código activo") }
        if (revokeConfirm) AlertDialog(onDismissRequest = { if (!busy) revokeConfirm = false },
            title = { Text("¿Revocar el código?") }, text = { Text("No podrá usarse para nuevas incorporaciones. Quienes ya se unieron conservan su vínculo.") },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                revokeConfirm = false; busy = true
                scope.launch {
                    try { repo.revokeTeamCode(teamId); code = null; revision++; message = "Código revocado." }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { message = "No se confirmó la revocación. Vuelve a cargar el equipo." }
                    finally { busy = false }
                }
            }) { Text("Sí, revocar") } }, dismissButton = { TextButton(onClick = { revokeConfirm = false }) { Text("Cancelar") } })
        Text("Incorporaciones registradas", style = MaterialTheme.typography.titleMedium)
        if (joiners.isEmpty()) Text("No hay incorporaciones disponibles en este registro.")
        joiners.forEach { person ->
            Text(person.name)
            if (person.joinedAtMillis > 0) Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(person.joinedAtMillis)), style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(enabled = !busy, onClick = { nav.safeBack() }) { Text("Volver") }
    }
}

@Composable
fun GuidedPlayerEditor(teamId: String, playerId: String, nav: NavHostController) {
    val session = LocalTeamSession.current
    val actor = session.account
    val admin = actor?.role == UserRole.ADMIN
    val allowed = !session.loading && session.error == null && RolePolicy.isStaff(actor?.role) && RolePolicy.canReadTeam(actor, teamId)
    val repo = remember { DefaultAppContainer().guidedAccounts }
    val players = remember { DefaultAppContainer().playerRepository }
    val scope = rememberCoroutineScope()
    var draft by remember(teamId, playerId, actor?.uid) { mutableStateOf<GuidedFormDraft?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var errors by remember { mutableStateOf(emptyMap<ProfileField, String>()) }
    var playerUid by remember { mutableStateOf("") }
    var targetTeam by remember { mutableStateOf("") }
    var moveConfirm by remember { mutableStateOf(false) }
    var removeConfirm by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(teamId, playerId, allowed, actor?.uid, refresh) {
        draft = null; playerUid = ""
        if (!allowed) return@LaunchedEffect
        busy = true
        try {
            draft = repo.loadPlayerData(teamId, playerId).asDraft()
            if (admin) {
                val result = players.getPlayerById(teamId, playerId).first { it !is Result.Loading }
                playerUid = (result as? Result.Success)?.data?.userUid.orEmpty()
            }
        } catch (e: CancellationException) { throw e }
          catch (e: Exception) { message = "No se pudo cargar la ficha con tu acceso actual." }
        finally { busy = false }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (admin) "Editar ficha" else "Datos deportivos", style = MaterialTheme.typography.headlineMedium)
        if (busy || session.loading) CircularProgressIndicator()
        if (!session.loading && !allowed) Text("No tienes acceso de staff a esta ficha.")
        message?.let { Text(it) }
        val current = draft
        if (current != null && allowed) {
            val access = if (admin) FormAccess.ADMIN else FormAccess.STAFF_SPORTS
            if (!admin) Text("Sólo puedes modificar posición y pie hábil. Los datos personales los actualiza el jugador.")
            GuidedDetailsForm(current, access, busy, errors) { draft = it; errors = emptyMap() }
            Button(enabled = !busy, onClick = {
                errors = current.errors(access)
                if (errors.isEmpty()) {
                    busy = true; message = null
                    scope.launch {
                        try {
                            if (admin) repo.savePlayerAsAdmin(teamId, playerId, current.asProfile())
                            else repo.savePlayerSports(teamId, playerId, checkNotNull(current.position), checkNotNull(current.foot).name)
                            message = "Cambios guardados."; session.refresh()
                        } catch (e: CancellationException) { throw e }
                          catch (e: Exception) { message = "No se confirmó el cambio. Actualiza la ficha y revisa tus permisos." }
                        finally { busy = false }
                    }
                }
            }) { Text("Guardar cambios") }
            OutlinedButton(enabled = !busy, onClick = { removeConfirm = true }) { Text("Retirar del equipo") }
            if (removeConfirm) AlertDialog(onDismissRequest = { if (!busy) removeConfirm = false },
                title = { Text("¿Retirar esta ficha del equipo?") },
                text = { Text("La ficha queda inactiva. No se borra el historial ni se impide que la persona se una a otro equipo.") },
                confirmButton = { TextButton(enabled = !busy, onClick = {
                    removeConfirm = false; busy = true; message = null
                    scope.launch {
                        try { repo.removePlayer(teamId, playerId); session.refresh(); nav.safeBack() }
                        catch (e: CancellationException) { throw e }
                        catch (e: Exception) { message = "No se confirmó el retiro. Revisa el estado actual de la ficha." }
                        finally { busy = false }
                    }
                }) { Text("Confirmar retiro") } },
                dismissButton = { TextButton(onClick = { removeConfirm = false }) { Text("Cancelar") } })
            if (admin && playerUid.isNotBlank()) {
                HorizontalDivider()
                Text("Trasladar a otro equipo", style = MaterialTheme.typography.titleMedium)
                Text("La ficha anterior queda inactiva; no se borra el historial.")
                session.teams.filter { it.id != teamId && it.isActive }.forEach { team ->
                    FilterChip(selected = targetTeam == team.id, onClick = { targetTeam = team.id },
                        enabled = !busy, label = { Text(team.name) })
                }
                OutlinedButton(enabled = !busy && targetTeam.isNotBlank(), onClick = { moveConfirm = true }) { Text("Trasladar jugador") }
                if (moveConfirm) AlertDialog(onDismissRequest = { if (!busy) moveConfirm = false },
                    title = { Text("¿Confirmar traslado?") }, text = { Text("Se conservará la ficha anterior inactiva y se creará la vinculación en el nuevo equipo.") },
                    confirmButton = { TextButton(enabled = !busy, onClick = {
                        moveConfirm = false; busy = true; message = null
                        scope.launch {
                            try { repo.movePlayerAsAdmin(playerUid, targetTeam); session.refresh(); nav.safeBack() }
                            catch (e: CancellationException) { throw e }
                            catch (e: Exception) { message = "No se confirmó el traslado. Vuelve a cargar la ficha." }
                            finally { busy = false }
                        }
                    }) { Text("Confirmar traslado") } },
                    dismissButton = { TextButton(onClick = { moveConfirm = false }) { Text("Cancelar") } })
            }
        } else if (!busy && allowed) OutlinedButton(onClick = { refresh++ }) { Text("Volver a cargar") }
        OutlinedButton(enabled = !busy, onClick = { nav.safeBack() }) { Text("Volver") }
    }
}
