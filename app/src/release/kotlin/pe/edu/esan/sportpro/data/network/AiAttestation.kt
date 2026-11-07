package pe.edu.esan.sportpro.data.network

/** No production attestation configured: release never initializes AI or sends prompts. */
internal object AiAttestation {
    suspend fun prepare() { error("IA remota no habilitada en release: falta atestación de producción") }
}
