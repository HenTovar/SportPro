package pe.edu.esan.sportpro.ui.common

import kotlinx.coroutines.withTimeout

/** UI operation budget, not a Storage configuration or permission change. */
suspend fun <T> boundedPhotoUpload(cancel: () -> Unit, upload: suspend () -> T): T = try {
    withTimeout(20_000) { upload() }
} catch (error: Exception) {
    cancel()
    throw error
}
