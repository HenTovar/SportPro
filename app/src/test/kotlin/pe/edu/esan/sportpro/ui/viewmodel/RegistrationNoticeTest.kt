package pe.edu.esan.sportpro.ui.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.kotlin.*
import pe.edu.esan.sportpro.data.model.*
import pe.edu.esan.sportpro.data.repository.AuthRepository
import pe.edu.esan.sportpro.data.repository.Result

@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationNoticeTest {
    private val repo:AuthRepository=mock()
    private lateinit var vm:AuthViewModel
    @Before fun setup(){
        Dispatchers.setMain(UnconfinedTestDispatcher())
        whenever(repo.isEmailValid(any())).thenReturn(true);whenever(repo.isPasswordValid(any())).thenReturn(true)
        vm=AuthViewModel(repo);vm.setEmail("a@example.test");vm.setPassword("secret12");vm.setName("Nombre")
    }
    @After fun cleanup(){Dispatchers.resetMain()}
    @Test fun successfulCoachRequestHasPendingNoticeAndBaseRole()=runTest {
        vm.setRole(UserRole.ENTRENADOR)
        whenever(repo.register(any(),any(),any(),eq(UserRole.ENTRENADOR))).thenReturn(flowOf(Result.Success(User(role=UserRole.JUGADOR))))
        vm.register()
        assertTrue(vm.uiState.value.registerSuccess)
        assertEquals(RolePolicy.coachPendingNotice,vm.uiState.value.registrationNotice)
        assertFalse(RolePolicy.isStaff(vm.uiState.value.user?.role))
        vm.clearSuccess();assertNull(vm.uiState.value.registrationNotice)
    }
    @Test fun failedSubmissionNeverClaimsRequestSent()=runTest {
        vm.setRole(UserRole.ENTRENADOR)
        whenever(repo.register(any(),any(),any(),eq(UserRole.ENTRENADOR))).thenReturn(flowOf(Result.Error("No se pudo enviar")))
        vm.register();assertFalse(vm.uiState.value.registerSuccess);assertNull(vm.uiState.value.registrationNotice)
        assertNotNull(vm.uiState.value.error)
    }
    @Test fun parentRegistrationDoesNotClaimPendingCoach()=runTest {
        vm.setRole(UserRole.PADRE)
        whenever(repo.register(any(),any(),any(),eq(UserRole.PADRE))).thenReturn(flowOf(Result.Success(User(role=UserRole.PADRE))))
        vm.register();assertTrue(vm.uiState.value.registerSuccess);assertNull(vm.uiState.value.registrationNotice)
    }
}
