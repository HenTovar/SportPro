package pe.edu.esan.sportpro.ui.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import pe.edu.esan.sportpro.data.model.User
import pe.edu.esan.sportpro.data.model.UserRole
import pe.edu.esan.sportpro.data.repository.AuthRepository
import pe.edu.esan.sportpro.data.repository.Result
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Pruebas unitarias para AuthViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth
    @Mock
    private lateinit var mockFirestore: FirebaseFirestore

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        // Create a real repository instance with mocked Firebase dependencies
        authRepository = AuthRepository(mockFirebaseAuth, mockFirestore)
        viewModel = AuthViewModel(authRepository)
    }

    @Test
    fun testEmailValidation_validEmail() {
        // Email válido
        assertThat(authRepository.isEmailValid("test@example.com")).isTrue()
    }

    @Test
    fun testEmailValidation_invalidEmail() {
        // Email inválido sin @
        assertThat(authRepository.isEmailValid("invalid-email")).isFalse()
    }

    @Test
    fun testEmailValidation_validEmailWithPlus() {
        // Email válido con +
        assertThat(authRepository.isEmailValid("test+tag@example.com")).isTrue()
    }

    @Test
    fun testPasswordValidation_validPassword() {
        // Contraseña válida (6 caracteres)
        assertThat(authRepository.isPasswordValid("password123")).isTrue()
    }

    @Test
    fun testPasswordValidation_invalidShort() {
        // Contraseña inválida (menos de 6)
        assertThat(authRepository.isPasswordValid("pass")).isFalse()
    }

    @Test
    fun testPasswordValidation_exactlyMinLength() {
        // Contraseña con exactamente 6 caracteres (el mínimo)
        assertThat(authRepository.isPasswordValid("123456")).isTrue()
    }

    @Test
    fun testSetEmail() {
        viewModel.setEmail("test@example.com")
        assertThat(viewModel.uiState.value.email).isEqualTo("test@example.com")
    }

    @Test
    fun testSetPassword() {
        viewModel.setPassword("password123")
        assertThat(viewModel.uiState.value.password).isEqualTo("password123")
    }

    @Test
    fun testSetName() {
        viewModel.setName("Juan Pérez")
        assertThat(viewModel.uiState.value.name).isEqualTo("Juan Pérez")
    }

    @Test
    fun testSetRole() {
        viewModel.setRole(UserRole.ENTRENADOR)
        assertThat(viewModel.uiState.value.selectedRole).isEqualTo(UserRole.ENTRENADOR)
    }

    @Test
    fun testLoginWithInvalidEmail() {
        viewModel.setEmail("invalid-email")
        viewModel.setPassword("password123")
        viewModel.login()

        assertThat(viewModel.uiState.value.error).isNotEmpty()
    }

    @Test
    fun testLoginWithEmptyFields() {
        viewModel.setEmail("")
        viewModel.setPassword("")
        viewModel.login()

        assertThat(viewModel.uiState.value.error).isNotEmpty()
    }

    @Test
    fun testRegisterWithInvalidPassword() {
        viewModel.setEmail("test@example.com")
        viewModel.setPassword("pass") // Menor a 6 caracteres
        viewModel.setName("Juan Pérez")
        viewModel.register()

        assertThat(viewModel.uiState.value.error).isNotEmpty()
    }

    @Test
    fun testClearError() {
        // Trigger an error first by setting invalid email
        viewModel.setEmail("invalid")
        viewModel.setPassword("password123")
        viewModel.login()

        // Verify error is set
        assertThat(viewModel.uiState.value.error).isNotEmpty()

        // Clear the error
        viewModel.clearError()

        // Verify error is cleared (set to null)
        assertThat(viewModel.uiState.value.error).isNull()
    }
}
