package pe.edu.esan.sportpro.ui.guided

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import pe.edu.esan.sportpro.data.model.*
import pe.edu.esan.sportpro.data.repository.GuidedAccounts
import pe.edu.esan.sportpro.data.repository.Result
import pe.edu.esan.sportpro.ui.screens.home.ProfileRoleUi

/** Principal reproduction of consumer behavior. JVM only, NOT rendered Android UI. */
@OptIn(ExperimentalCoroutinesApi::class)
class GuidedClientAcceptanceTest {
    @Before fun setDispatcher() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun resetDispatcher() { Dispatchers.resetMain() }
    private class Client : GuidedAccounts {
        var calls=mutableListOf<String>();var fail=false
        var p=GuidedProfile(name="Adulto ficticio",phone="0000000",dateOfBirth=946684800000,
            position=PlayerPosition.PORTERO,dominantFoot="LEFT")
        override fun register(email:String,password:String,profile:GuidedProfile)=flowOf<Result<User>>(Result.Success(User(uid="fixture",role=UserRole.JUGADOR)))
        override suspend fun loadMyData()=p
        override suspend fun saveMyData(profile:GuidedProfile) { p=profile;calls+="save" }
        override suspend fun joinTeam(code:String) { calls+="join:$code";if(fail)error("PRIVATE_BACKEND_DETAIL") }
        override suspend fun leaveTeam(expectedTeamId:String) { calls+="leave:$expectedTeamId" }
        override suspend fun generateTeamCode(teamId:String)="fixture"
        override suspend fun loadPlayerData(teamId:String,playerId:String)=p
        override suspend fun savePlayerSports(teamId:String,playerId:String,position:PlayerPosition,dominantFoot:String)=Unit
        override suspend fun savePlayerAsAdmin(teamId:String,playerId:String,profile:GuidedProfile)=Unit
        override suspend fun movePlayerAsAdmin(userUid:String,targetTeamId:String)=Unit
        override suspend fun revokeTeamCode(teamId:String)=Unit
        override suspend fun loadTeamJoiners(teamId:String)=emptyList<TeamJoiner>()
        override suspend fun removePlayer(teamId:String,playerId:String)=Unit
    }
    @Test fun clientCanSaveThenJoinThenLeaveWithoutAStaffParameter()=runTest {
        val c=Client();val vm=GuidedAccountViewModel(c);vm.load();advanceUntilIdle()
        vm.change(vm.state.value.draft.copy(phone="0000001"));vm.save();advanceUntilIdle()
        vm.join(" ABCDEFGH2345 ");advanceUntilIdle();vm.leave("teamA");advanceUntilIdle()
        assertEquals(listOf("save","join:ABCDEFGH2345","leave:teamA"),c.calls)
        assertEquals(3,vm.state.value.revision)
    }
    @Test fun failedJoinDoesNotPretendMembershipChanged()=runTest {
        val c=Client().apply {fail=true};val vm=GuidedAccountViewModel(c);vm.load();advanceUntilIdle()
        vm.join("ABCDEFGH2345");advanceUntilIdle()
        assertEquals(0,vm.state.value.revision);assertFalse(vm.state.value.busy)
        assertFalse(vm.state.value.message.orEmpty().contains("PRIVATE_BACKEND_DETAIL"))
    }
    @Test fun pendingCoachRetainsFieldsAndNoEffectiveCoachRole()=runTest {
        val c=Client().apply {p=GuidedProfile(requestedRole=UserRole.ENTRENADOR,name="Adulto",phone="0000000",specialtyOrLicense="Licencia",yearsExperience=0,coachPending=true)}
        val vm=GuidedAccountViewModel(c);vm.load();advanceUntilIdle()
        assertTrue(vm.state.value.coachPending);assertEquals("Licencia",vm.state.value.draft.specialtyOrLicense)
        assertEquals(UserRole.JUGADOR,RolePolicy.registrationBaseRole(checkNotNull(vm.state.value.draft.role)))
    }
    @Test fun staffOnlySportsSelfOnlyPersonalAndAdminNeverEditsRole() {
        val d=GuidedFormDraft(role=UserRole.JUGADOR)
        assertEquals(setOf(ProfileField.POSITION,ProfileField.FOOT),ProfileField.entries.filter {d.canEdit(it,FormAccess.STAFF_SPORTS)}.toSet())
        for(f in listOf(ProfileField.ROLE,ProfileField.POSITION,ProfileField.FOOT))assertFalse(d.canEdit(f,FormAccess.SELF))
        for(f in listOf(ProfileField.NAME,ProfileField.PHONE,ProfileField.BIRTH_DATE))assertTrue(d.canEdit(f,FormAccess.SELF))
        assertFalse(d.canEdit(ProfileField.ROLE,FormAccess.ADMIN))
    }
    @Test fun switchingRoleErasesHiddenFieldsAndAdminChoiceFails() {
        val old=GuidedFormDraft(role=UserRole.JUGADOR,name="Adulto",phone="0000000",birthDate="2000-01-01",position=PlayerPosition.PORTERO,foot=FootChoice.LEFT)
        val next=old.selectRole(UserRole.PADRE)
        assertEquals("",next.birthDate);assertNull(next.position);assertNull(next.foot);assertEquals(old.name,next.name)
        try {old.selectRole(UserRole.ADMIN);fail("ADMIN registration")}catch(_:IllegalArgumentException) { }
    }
    @Test fun unknownRoleNeverDefaultsToPlayer() {
        assertEquals("Rol: Cargando…",ProfileRoleUi.roleText(null,true,false))
        assertEquals("Rol: No disponible",ProfileRoleUi.roleText(null,false,true))
        assertFalse(ProfileRoleUi.canRequest(null,UserRole.ENTRENADOR,false,false,false,false))
    }
}
