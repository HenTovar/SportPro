package pe.edu.esan.sportpro.ui.guided

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import pe.edu.esan.sportpro.data.model.*
import pe.edu.esan.sportpro.data.repository.GuidedAccounts
import pe.edu.esan.sportpro.data.repository.Result

@OptIn(ExperimentalCoroutinesApi::class)
class GuidedAccountViewModelTest {
    @Before fun main() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun reset() { Dispatchers.resetMain() }
    private fun player() = GuidedProfile(requestedRole = UserRole.JUGADOR, name = "Persona de prueba", phone = "900000000",
        dateOfBirth = 946684800000L, position = PlayerPosition.DELANTERO, dominantFoot = "RIGHT")
    private class Fake(var profile: GuidedProfile) : GuidedAccounts {
        var registrations = 0; var joins = 0; var leaves = mutableListOf<String>(); var saves = 0
        var failLoad = false; var failJoin = false
        override fun register(email: String, password: String, profile: GuidedProfile) = flow<Result<User>> {
            registrations++; emit(Result.Loading); emit(Result.Success(User(uid = "test-user", role = UserRole.JUGADOR)))
        }
        override suspend fun loadMyData(): GuidedProfile { if (failLoad) error("internal-load-error"); return profile }
        override suspend fun saveMyData(profile: GuidedProfile) { saves++; this.profile = profile }
        override suspend fun joinTeam(code: String) { joins++; delay(100); if (failJoin) error("internal-must-not-display") }
        override suspend fun leaveTeam(expectedTeamId: String) { leaves += expectedTeamId }
        override suspend fun generateTeamCode(teamId: String): String = error("not exercised")
        override suspend fun loadPlayerData(teamId: String, playerId: String): GuidedProfile = error("not exercised")
        override suspend fun savePlayerSports(teamId: String, playerId: String, position: PlayerPosition, dominantFoot: String) = Unit
        override suspend fun savePlayerAsAdmin(teamId: String, playerId: String, profile: GuidedProfile) = Unit
        override suspend fun movePlayerAsAdmin(userUid: String, targetTeamId: String) = Unit
        override suspend fun revokeTeamCode(teamId: String) = Unit
        override suspend fun loadTeamJoiners(teamId: String): List<TeamJoiner> = emptyList()
        override suspend fun removePlayer(teamId: String, playerId: String) = Unit
    }
    @Test fun coachRegistrationIsPendingWithoutChangingEffectiveRole() = runTest {
        val repo = Fake(player()); val vm = GuidedAccountViewModel(repo)
        vm.register(GuidedFormDraft(role = UserRole.ENTRENADOR, name = "Persona de prueba", phone = "900000000",
            specialtyOrLicense = "Entrenamiento", yearsExperience = "0"), "persona@example.invalid", "test-only-password")
        advanceUntilIdle()
        assertEquals(1, repo.registrations); assertTrue(vm.state.value.registered); assertTrue(vm.state.value.coachPending)
        assertFalse(vm.state.value.busy)
    }
    @Test fun invalidFormNeverCreatesAuthUser() = runTest {
        val repo = Fake(player()); val vm = GuidedAccountViewModel(repo)
        vm.register(GuidedFormDraft(role = UserRole.ADMIN), "persona@example.invalid", "test-only-password")
        advanceUntilIdle(); assertEquals(0, repo.registrations); assertFalse(vm.state.value.registered)
    }
    @Test fun doubleJoinIsOneOperationAndRefreshesOnlyAfterSuccess() = runTest {
        val repo = Fake(player()); val vm = GuidedAccountViewModel(repo)
        vm.load(); advanceUntilIdle(); vm.join("TEAMCODE"); vm.join("TEAMCODE")
        advanceUntilIdle(); assertEquals(1, repo.joins); assertEquals(1, vm.state.value.revision)
    }
    @Test fun unsavedPersonalDataMustBeSavedBeforeJoin() = runTest {
        val repo = Fake(player()); val vm = GuidedAccountViewModel(repo)
        vm.load(); advanceUntilIdle(); vm.change(vm.state.value.draft.copy(name = "Nombre actualizado")); vm.join("TEAMCODE")
        advanceUntilIdle(); assertEquals(0, repo.joins)
        vm.save(); advanceUntilIdle(); vm.join("TEAMCODE"); advanceUntilIdle()
        assertEquals(1, repo.saves); assertEquals(1, repo.joins)
    }
    @Test fun leaveUsesExpectedTeamAndRefreshesAfterConfirmedSuccess() = runTest {
        val repo = Fake(player()); val vm = GuidedAccountViewModel(repo)
        vm.load(); advanceUntilIdle(); vm.leave("team-a"); advanceUntilIdle()
        assertEquals(listOf("team-a"), repo.leaves); assertEquals(1, vm.state.value.revision)
    }
    @Test fun failedLoadNeverEnablesMembershipOperation() = runTest {
        val repo = Fake(player()).apply { failLoad = true }; val vm = GuidedAccountViewModel(repo)
        vm.load(); advanceUntilIdle(); vm.join("TEAMCODE"); vm.leave("team-a"); advanceUntilIdle()
        assertFalse(vm.state.value.loaded); assertEquals(0, repo.joins); assertTrue(repo.leaves.isEmpty())
    }
    @Test fun failedJoinDoesNotRefreshAndDoesNotExposeRawException() = runTest {
        val repo = Fake(player()).apply { failJoin = true }; val vm = GuidedAccountViewModel(repo)
        vm.load(); advanceUntilIdle(); vm.join("TEAMCODE"); advanceUntilIdle()
        assertEquals(0, vm.state.value.revision); assertFalse(vm.state.value.busy)
        assertFalse(vm.state.value.message.orEmpty().contains("internal-must-not-display"))
    }
    @Test fun profileMappingPreservesPersonalAndSportsFields() {
        val expected = player().copy(heightCm = 170.5, weightKg = 60.25)
        assertEquals(expected, expected.asDraft().asProfile(expected))
        assertEquals("2000-01-01", expected.asDraft().birthDate)
    }
}
