package pe.edu.esan.sportpro.ui.guided

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.UploadTask
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import pe.edu.esan.sportpro.data.model.User
import pe.edu.esan.sportpro.ui.common.boundedPhotoUpload

/** Active guided editor entry. Upload uses the existing SDK and 20 s cancellation budget. */
@Composable
internal fun GuidedPlayerPhoto(teamId: String, playerId: String, actor: User?, photoUrl: String?,
                               busy: Boolean, onBusy: (Boolean) -> Unit, onSaved: (String?) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val storage = remember { FirebaseStorage.getInstance() }
    val currentActor by rememberUpdatedState(actor)
    val currentBusy by rememberUpdatedState(busy)
    val currentPhoto by rememberUpdatedState(photoUrl)
    val currentOnBusy by rememberUpdatedState(onBusy)
    val currentOnSaved by rememberUpdatedState(onSaved)
    var preview by remember(teamId, playerId, actor?.uid) { mutableStateOf<Uri?>(null) }
    var uploading by remember(teamId, playerId, actor?.uid) { mutableStateOf(false) }
    var picking by remember(teamId, playerId, actor?.uid) { mutableStateOf(false) }
    var uncertain by remember(teamId, playerId, actor?.uid) { mutableStateOf(false) }
    var message by remember(teamId, playerId, actor?.uid) { mutableStateOf<String?>(null) }
    val canManage = PlayerPhotoPolicy.canManage(actor, teamId, playerId)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        picking = false
        if (uri != null && !currentBusy && !uncertain &&
            PlayerPhotoPolicy.canManage(currentActor, teamId, playerId)) {
            val expectedActor = checkNotNull(currentActor).uid
            val previousPhoto = currentPhoto
            uploading = true; currentOnBusy(true); preview = uri; message = null
            scope.launch {
                var upload: UploadTask? = null
                try {
                    val url = boundedPhotoUpload(cancel = { upload?.cancel() }) {
                        check(auth.currentUser?.uid == expectedActor) { "La sesión cambió." }
                        val liveActor = db.collection("users").document(expectedActor).get(Source.SERVER).await()
                        check(liveActor.getString("role") == "ADMIN" && liveActor.getBoolean("active") != false)
                        val mime = context.contentResolver.getType(uri)
                        val size = withContext(Dispatchers.IO) {
                            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                                val column = cursor.getColumnIndex(OpenableColumns.SIZE)
                                if (column >= 0 && cursor.moveToFirst() && !cursor.isNull(column)) cursor.getLong(column).takeIf { it >= 0 } else null
                            }
                        }
                        require(PlayerPhotoPolicy.validImage(mime, size)) { "Elige una imagen de menos de 5 MB." }
                        val path = PlayerPhotoPolicy.objectPath(teamId, playerId, UUID.randomUUID().toString())
                        val reference = storage.reference.child(path)
                        // No synthetic authorization metadata, claims or rule changes.
                        val metadata = StorageMetadata.Builder().setContentType(checkNotNull(mime)).build()
                        upload = reference.putFile(uri, metadata)
                        checkNotNull(upload).await()
                        val downloaded = reference.downloadUrl.await().toString()
                        check(auth.currentUser?.uid == expectedActor) { "La sesión cambió." }
                        val player = db.collection("teams").document(teamId).collection("players").document(playerId)
                        db.runTransaction { tx ->
                            val before = tx.get(player)
                            check(before.exists() && before.getBoolean("active") != false) { "La ficha ya no está activa." }
                            check(before.getString("photoUrl").orEmpty() == previousPhoto.orEmpty()) { "La foto cambió. Comprueba la ficha antes de reintentar." }
                            tx.update(player, mapOf("photoUrl" to downloaded, "updatedAt" to FieldValue.serverTimestamp()))
                        }.await()
                        downloaded
                    }
                    currentOnSaved(url); preview = null
                    message = "Foto guardada en la ficha. Tus otros cambios del formulario se conservan sin guardar."
                } catch (_: TimeoutCancellationException) {
                    upload?.cancel(); preview = null; uncertain = true
                    message = "No se confirmó la foto en 20 segundos. Comprueba la ficha antes de volver a elegir otra."
                } catch (e: CancellationException) {
                    upload?.cancel()
                    throw e
                } catch (_: Exception) {
                    upload?.cancel(); preview = null; uncertain = true
                    message = "No se confirmó el cambio de foto. Revisa conexión y permisos de Storage; comprueba la ficha antes de reintentar. No se cambiaron las reglas."
                } finally {
                    uploading = false; currentOnBusy(false)
                }
            }
        }
    }
    BackHandler(enabled = uploading) { }

    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Foto del jugador", style = MaterialTheme.typography.titleMedium)
            val image: Any? = preview ?: photoUrl?.takeIf { it.isNotBlank() }
            if (image != null) AsyncImage(model = image, contentDescription = "Foto de la ficha del jugador",
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(180.dp))
            else Text("Esta ficha no tiene una foto guardada.")
            if (canManage) {
                Text("Elige una imagen de menos de 5 MB. Se guarda por separado de los datos del formulario.",
                    style = MaterialTheme.typography.bodySmall)
                Button(onClick = {
                    picking = true
                    try { picker.launch("image/*") }
                    catch (_: Exception) {
                        picking = false
                        message = "No se pudo abrir el selector de imágenes. Usa una aplicación de galería disponible."
                    }
                }, enabled = !busy && !picking && !uncertain,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (uploading) "Guardando foto…" else if (picking) "Eligiendo foto…" else "Elegir y guardar foto")
                }
                if (uncertain) OutlinedButton(enabled = !busy, onClick = {
                    currentOnBusy(true)
                    scope.launch {
                        try {
                            check(PlayerPhotoPolicy.canManage(currentActor, teamId, playerId))
                            check(auth.currentUser?.uid == checkNotNull(currentActor).uid)
                            val row = withTimeout(10_000) {
                                db.collection("teams").document(teamId).collection("players").document(playerId)
                                    .get(Source.SERVER).await()
                            }
                            check(row.exists())
                            currentOnSaved(row.getString("photoUrl")); uncertain = false
                            message = "Foto actual de la ficha comprobada. Puedes elegir otra si lo necesitas."
                        } catch (_: TimeoutCancellationException) {
                            message = "No se pudo comprobar la foto en 10 segundos. Conéctate antes de reintentar."
                        } catch (e: CancellationException) { throw e }
                        catch (_: Exception) { message = "No se pudo comprobar la foto. Revisa tu conexión y acceso a esta ficha." }
                        finally { currentOnBusy(false) }
                    }
                }) { Text("Comprobar foto guardada") }
            } else Text("Sólo administración puede cambiar la foto. El entrenador conserva la edición de posición y pie hábil.",
                style = MaterialTheme.typography.bodySmall)
            if (uploading) LinearProgressIndicator(Modifier.fillMaxWidth())
            message?.let { Text(it) }
        }
    }
}
