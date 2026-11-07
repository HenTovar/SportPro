package pe.edu.esan.sportpro.data.network

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import pe.edu.esan.sportpro.data.model.*

/** Only this anonymous text crosses the provider boundary. No key or custom endpoint. */
fun interface NarrativeProvider { suspend fun generate(anonymousPrompt: String): String }
/** Consent is per generation; the only enabled product scope is synthetic academic data. */
internal object AcademicSummaryPolicy {
    fun allowRemote(confirmedFacts:Boolean, testDataConfirmed:Boolean)=confirmedFacts && testDataConfirmed
}
class FirebaseNarrativeProvider : NarrativeProvider {
    override suspend fun generate(anonymousPrompt: String): String {
        AiAttestation.prepare()
        val model=Firebase.ai(backend=GenerativeBackend.googleAI()).generativeModel(
            modelName="gemini-3.5-flash-lite",
            generationConfig=generationConfig { temperature=0f;maxOutputTokens=1000 })
        return model.generateContent(anonymousPrompt).text.orEmpty()
    }
}
enum class NarrativeSource { AI, TEMPLATE }
data class GeneratedNarrative(val text: String,val source: NarrativeSource,val notice: String)
class NarrativeGenerator(private val provider: NarrativeProvider=FirebaseNarrativeProvider(),private val timeoutMillis:Long=20_000) {
    suspend fun generate(match:LiveMatch,events:List<MatchEvent>,allowRemote:Boolean=true):GeneratedNarrative {
        val plan=AnonymousNarrative.prepare(match,events)
        if(!allowRemote || plan.eventCount==0 || plan.eventCount>200)return fallback(match,events)
        return try {
            val response=withTimeoutOrNull(timeoutMillis){provider.generate(plan.prompt)}
            val text=response?.let{plan.restore(it)}
            if(text==null)fallback(match,events) else GeneratedNarrative(text,NarrativeSource.AI,
                "Borrador con IA. Revisa hechos y lenguaje antes de aprobar.")
        } catch(e:CancellationException){throw e}
        catch(_:Exception){fallback(match,events)}
    }
    private fun fallback(match:LiveMatch,events:List<MatchEvent>)=GeneratedNarrative(
        AnonymousNarrative.template(match,events),NarrativeSource.TEMPLATE,
        "Resumen por plantilla: modo local, límite de hechos o proveedor no disponible; revisión pendiente.")
}
