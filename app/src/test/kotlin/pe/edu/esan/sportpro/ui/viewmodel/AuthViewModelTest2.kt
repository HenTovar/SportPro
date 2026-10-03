package pe.edu.esan.sportpro.ui.viewmodel

import org.junit.Test
import org.junit.Assert.*
import pe.edu.esan.sportpro.data.model.User
import pe.edu.esan.sportpro.data.model.UserRole
import pe.edu.esan.sportpro.data.repository.Result

/**
 * Unit tests for email validation.
 */
class AuthViewModelTest2 {

    @Test
    fun emailValidation_validEmail() {
        assertTrue(isEmailValid("user@example.com"))
    }

    @Test
    fun emailValidation_invalidEmail_noAt() {
        assertFalse(isEmailValid("userexample.com"))
    }

    @Test
    fun emailValidation_invalidEmail_empty() {
        assertFalse(isEmailValid(""))
    }

    @Test
    fun emailValidation_validEmail_withPlus() {
        assertTrue(isEmailValid("user+tag@example.com"))
    }

    @Test
    fun emailValidation_validEmail_withDot() {
        assertTrue(isEmailValid("user.name@example.com"))
    }

    @Test
    fun emailValidation_invalidEmail_onlyAt() {
        assertFalse(isEmailValid("user@"))
    }

    private fun isEmailValid(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$"
        return email.matches(emailRegex.toRegex())
    }
}

/**
 * Fake implementation of AuthRepository for testing without Firebase.
 */
class FakeAuthRepository(
    private val shouldFail: Boolean = false
) {
    fun register(email: String, password: String, name: String, role: UserRole): Result<User> {
        return if (email.isBlank()) {
            Result.Error("Email requerido")
        } else if (password.length < 6) {
            Result.Error("Contraseña mínimo 6 caracteres")
        } else if (shouldFail) {
            Result.Error("Error de registro")
        } else {
            Result.Success(User(
                uid = "test-uid",
                email = email,
                name = name,
                role = role,
                createdAt = System.currentTimeMillis()
            ))
        }
    }

    fun login(email: String, password: String): Result<User> {
        return if (email.isBlank() || password.isBlank()) {
            Result.Error("Email y contraseña requeridos")
        } else if (shouldFail) {
            Result.Error("Credenciales inválidas")
        } else {
            Result.Success(User(
                uid = "test-uid",
                email = email,
                name = "Test User",
                role = UserRole.JUGADOR,
                createdAt = System.currentTimeMillis()
            ))
        }
    }

    fun isPasswordValid(password: String): Boolean {
        return password.length >= 6
    }
}

/**
 * Unit tests for FakeAuthRepository.
 */
class AuthRepositoryFakeTest {
    private val repo = FakeAuthRepository()
    private val failingRepo = FakeAuthRepository(shouldFail = true)

    @Test
    fun register_success() {
        val result = repo.register("test@test.com", "password123", "Test", UserRole.JUGADOR)
        assertTrue(result is Result.Success)
        (result as Result.Success).data.apply {
            assertEquals("test@test.com", email)
            assertEquals("Test", name)
        }
    }

    @Test
    fun register_emptyEmail() {
        val result = repo.register("", "password123", "Test", UserRole.JUGADOR)
        assertTrue(result is Result.Error)
    }

    @Test
    fun register_shortPassword() {
        val result = repo.register("test@test.com", "pass", "Test", UserRole.JUGADOR)
        assertTrue(result is Result.Error)
    }

    @Test
    fun register_failure() {
        val result = failingRepo.register("test@test.com", "password123", "Test", UserRole.JUGADOR)
        assertTrue(result is Result.Error)
    }

    @Test
    fun login_success() {
        val result = repo.login("test@test.com", "password123")
        assertTrue(result is Result.Success)
        (result as Result.Success).data.apply {
            assertEquals("test@test.com", email)
        }
    }

    @Test
    fun login_emptyFields() {
        val result = repo.login("", "password")
        assertTrue(result is Result.Error)
    }

    @Test
    fun login_emptyPassword() {
        val result = repo.login("test@test.com", "")
        assertTrue(result is Result.Error)
    }

    @Test
    fun passwordValidation_validPassword() {
        assertTrue(repo.isPasswordValid("password123"))
    }

    @Test
    fun passwordValidation_shortPassword() {
        assertFalse(repo.isPasswordValid("12345"))
    }

    @Test
    fun login_failure() {
        val result = failingRepo.login("test@test.com", "password123")
        assertTrue(result is Result.Error)
    }
}
