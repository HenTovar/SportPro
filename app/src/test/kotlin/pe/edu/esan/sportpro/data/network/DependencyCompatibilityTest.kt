package pe.edu.esan.sportpro.data.network

import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import com.google.firestore.v1.Document
import com.google.firestore.v1.Value
import org.junit.Assert.*
import org.junit.Test

/** Ejecuta APIs transitivas usadas por Firebase, sin red ni datos reales. */
class DependencyCompatibilityTest {
    @Test fun firestoreGeneratedMessagesRoundTripWithSelectedLiteRuntime() {
        val input = Document.newBuilder()
            .setName("projects/demo-sportpro/databases/(default)/documents/tests/synthetic")
            .putFields("score", Value.newBuilder().setIntegerValue(3).build())
            .putFields("label", Value.newBuilder().setStringValue("TEAM_1").build())
            .build()
        val restored = Document.parseFrom(input.toByteArray())
        assertEquals(input, restored)
        assertEquals(3L, restored.getFieldsOrThrow("score").integerValue)
        assertEquals("TEAM_1", restored.getFieldsOrThrow("label").stringValue)
    }
    @Test fun androidGuavaCollectionsAndFutureTransformRemainCompatible() {
        val source = ImmutableList.of("TEAM_1", "TEAM_2")
        val result = Futures.transform(Futures.immediateFuture(source),
            { items -> requireNotNull(items).size }, MoreExecutors.directExecutor())
        assertEquals(2, result.get().toInt())
        assertEquals(listOf("TEAM_1", "TEAM_2"), source)
    }
}
