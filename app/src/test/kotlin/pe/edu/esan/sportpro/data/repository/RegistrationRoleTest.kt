package pe.edu.esan.sportpro.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.*
import pe.edu.esan.sportpro.data.model.UserRole

class RegistrationRoleTest {
    @Test fun adminIsRejectedBeforeCreatingAuthAccountOrWritingProfile() = runTest {
        val auth = mock<FirebaseAuth>()
        val db = mock<FirebaseFirestore>()
        val result = AuthRepository(auth, db).register("test@example.com", "password", "Test", UserRole.ADMIN).toList().last()
        assertTrue(result is Result.Error)
        verifyNoInteractions(auth, db)
    }
}
