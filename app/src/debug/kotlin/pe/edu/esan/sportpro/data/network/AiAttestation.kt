package pe.edu.esan.sportpro.data.network

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import kotlinx.coroutines.tasks.await

/** Only the owner's registered debug device. Never expose a token or continue without attestation. */
internal object AiAttestation {
    private var installed=false
    @Synchronized private fun install() {
        if(!installed) {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
            installed=true
        }
    }
    suspend fun prepare() {
        install()
        val token=FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
        check(token.token.isNotBlank()) { "Atestación no disponible" }
    }
}
