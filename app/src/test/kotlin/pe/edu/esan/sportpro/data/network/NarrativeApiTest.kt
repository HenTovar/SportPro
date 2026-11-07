package pe.edu.esan.sportpro.data.network

import com.google.gson.Gson
import java.net.ServerSocket
import kotlin.concurrent.thread
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import pe.edu.esan.sportpro.data.model.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NarrativeApiTest {
    @Test fun retrofitSendsRecordedFactsToSimulatedProviderAndReadsDraft() = runBlocking {
        val received=AtomicReference<String>()
        val server=ServerSocket(0,1,InetAddress.getByName("127.0.0.1"))
        server.soTimeout=10000
        val worker=thread(isDaemon=true) {
            server.accept().use { socket ->
                socket.soTimeout=10000
                val input=socket.getInputStream()
                fun line():String { val bytes=java.io.ByteArrayOutputStream()
                    while(true){val b=input.read();check(b>=0);if(b==10)break;if(b!=13)bytes.write(b)}
                    return bytes.toString("UTF-8") }

                val requestLine=line();check(requestLine=="POST /v1/chat/completions HTTP/1.1")
                val headers=mutableMapOf<String,String>()
                while(true) {val line=line();if(line.isNullOrEmpty())break
                    headers[line.substringBefore(':').lowercase()]=line.substringAfter(':').trim()}
                check(headers["authorization"]=="Bearer test-only")
                val body=ByteArray(headers.getValue("content-length").toInt());var offset=0
                while(offset<body.size){val n=input.read(body,offset,body.size-offset);check(n>0);offset+=n}
                received.set(String(body))
                val result="""{"choices":[{"message":{"role":"assistant","content":"Borrador del gol registrado."}}]}""".toByteArray()
                val output=socket.getOutputStream()
                output.write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${result.size}\r\nConnection: close\r\n\r\n".toByteArray())
                output.write(result);output.flush()
            }
        }
        try {
            val api=Retrofit.Builder().baseUrl("http://127.0.0.1:${server.localPort}/v1/")
                .addConverterFactory(GsonConverterFactory.create()).build().create(NarrativeApi::class.java)
            val match=LiveMatch(teamA="a",teamB="b",status="in_progress")
            val goal=MatchEvent(id="g",typeLabel="Gol",countsGoal=true,teamId="a")
            val response=api.generate("Bearer test-only",NarrativeContract.request("test-model",match,listOf(goal,goal.copy(id="void",voided=true))))
            assertEquals("Borrador del gol registrado.",NarrativeContract.text(response))
            val request=Gson().fromJson(received.get(),AiRequest::class.java)
            assertEquals("test-model",request.model);assertEquals(2,request.messages.size)
            assertTrue(request.messages[0].content.contains("No inventes"))
            assertTrue(request.messages[1].content.contains("Evento g"));assertFalse(request.messages[1].content.contains("Evento void"))
            assertTrue(request.messages[1].content.contains("desconocido"))
        } finally {server.close();worker.join(1000)}
    }
    @Test fun emptyMissingAndOversizedResponsesFailRatherThanInventingSummary() {
        for(response in listOf(AiResponse(null),AiResponse(emptyList()),AiResponse(listOf(AiChoice(null))),AiResponse(listOf(AiChoice(AiMessage("assistant"," ")))),AiResponse(listOf(AiChoice(AiMessage("assistant","x".repeat(8001))))))) {
            try {NarrativeContract.text(response);fail("invalid response accepted")}catch(expected:IllegalStateException){}
        }
    }
}
