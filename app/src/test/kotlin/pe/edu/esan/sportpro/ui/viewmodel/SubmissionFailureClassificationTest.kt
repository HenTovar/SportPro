package pe.edu.esan.sportpro.ui.viewmodel
import org.junit.Test
import org.junit.Assert.*
import java.io.IOException
class SubmissionFailureClassificationTest {
    @Test fun definitiveDenialCodesAreRejected() {
        for(code in listOf("PERMISSION_DENIED","UNAUTHENTICATED","INVALID_ARGUMENT","FAILED_PRECONDITION","NOT_FOUND","ALREADY_EXISTS","OUT_OF_RANGE","ABORTED"))
            assertEquals(code,SubmissionPhase.REJECTED,firestoreFailurePhase(code))
    }
    @Test fun deadlineTransportAndUnknownCodesRemainUncertain() {
        for(code in listOf("DEADLINE_EXCEEDED","UNAVAILABLE","UNKNOWN","INTERNAL","CANCELLED","DATA_LOSS","RESOURCE_EXHAUSTED","NEW_UNKNOWN_CODE"))
            assertEquals(code,SubmissionPhase.UNCERTAIN,firestoreFailurePhase(code))
    }
    @Test fun validationCanBeRejectedButUnexpectedRuntimeFailureCannot() {
        assertEquals(SubmissionPhase.REJECTED,writeFailure(IllegalArgumentException("Invalid")))
        assertEquals(SubmissionPhase.UNCERTAIN,writeFailure(IllegalStateException("SDK state")))
        assertEquals(SubmissionPhase.UNCERTAIN,writeFailure(IOException("Lost ack")))
    }
}
