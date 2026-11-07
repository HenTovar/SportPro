package pe.edu.esan.sportpro.ui.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.currentTime
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PhotoUploadBudgetTest {
    @Test fun successReturnsWithoutCancelling() = runTest {
        var cancelled = false
        assertEquals("photo", boundedPhotoUpload({cancelled=true}) { "photo" })
        assertFalse(cancelled)
    }
    @Test fun unavailableStorageCannotWaitForever() = runTest {
        var cancelled = false
        try {
            boundedPhotoUpload({cancelled=true}) { delay(20_001); "unreachable" }
            fail("upload must time out")
        } catch (_: TimeoutCancellationException) { }
        assertTrue(cancelled)
        assertEquals(20_000L, currentTime)
    }
    @Test fun permissionFailureCancelsAndIsNotConvertedToSuccess() = runTest {
        var cancelled = false
        try { boundedPhotoUpload({cancelled=true}) { throw IllegalStateException("not available") }; fail("failure required") }
        catch (_: IllegalStateException) { }
        assertTrue(cancelled)
    }
    @Test fun leavingTheScreenPreservesCancellation() = runTest {
        var cancelled = false
        try { boundedPhotoUpload({cancelled=true}) { throw CancellationException("screen left") }; fail("cancellation required") }
        catch (_: CancellationException) { }
        assertTrue(cancelled)
    }
}
