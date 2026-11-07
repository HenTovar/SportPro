package pe.edu.esan.sportpro.ui.navigation

import org.junit.Assert.*
import org.junit.Test
import pe.edu.esan.sportpro.data.model.*

class StructurePolicyTest {
    private val teams = listOf(Team(id="a",name="A"), Team(id="b",name="B"), Team(id="old",isActive=false))
    private fun user(role:UserRole,team:String="",player:String="") = User(uid="one",role=role,teamId=team,playerId=player)
    @Test fun staffGetsFiveTabs() { for(role in listOf(UserRole.ADMIN,UserRole.ENTRENADOR)) assertEquals(5,StructurePolicy.destinations(user(role)).size) }
    @Test fun assignedPlayerAndParentGetFiveTabs() { for(role in listOf(UserRole.JUGADOR,UserRole.PADRE)) assertEquals(5,StructurePolicy.destinations(user(role,"a")).size) }
    @Test fun unassignedOnlyGetsHomeAndProfile() { assertEquals(listOf("Inicio","Perfil"),StructurePolicy.destinations(user(UserRole.JUGADOR))) }
    @Test fun signedOutDoesNotGetTeamActions() { assertEquals(2,StructurePolicy.destinations(null).size);assertNull(StructurePolicy.selectedTeam(null,"a",teams)) }
    @Test fun inactiveDoesNotExposeSelection() { val p=user(UserRole.ADMIN).copy(isActive=false);assertEquals(2,StructurePolicy.destinations(p).size);assertNull(StructurePolicy.selectedTeam(p,"a",teams)) }
    @Test fun staffLastTeamIsRestored() { assertEquals("b",StructurePolicy.selectedTeam(user(UserRole.ENTRENADOR).copy(coachTeamIds=listOf("a","b")),"b",teams)?.id) }
    @Test fun deletedSelectionFallsBackToVisibleTeam() { assertEquals("a",StructurePolicy.selectedTeam(user(UserRole.ADMIN),"missing",teams)?.id) }
    @Test fun inactiveTeamIsNotRestored() { assertEquals("a",StructurePolicy.selectedTeam(user(UserRole.ADMIN),"old",teams)?.id) }
    @Test fun emptyTeamsHasNoSelection() { assertNull(StructurePolicy.selectedTeam(user(UserRole.ADMIN),"a",emptyList())) }
    @Test fun playerCannotRestoreAnotherTeam() { assertEquals("a",StructurePolicy.selectedTeam(user(UserRole.JUGADOR,"a","p"),"b",teams)?.id) }
    @Test fun parentCannotRestoreAnotherTeam() { assertEquals("b",StructurePolicy.selectedTeam(user(UserRole.PADRE,"b","p"),"a",teams)?.id) }
    @Test fun unassignedCannotUseStaffPreference() { assertNull(StructurePolicy.selectedTeam(user(UserRole.JUGADOR),"a",teams)) }
    @Test fun missingAssignedTeamDoesNotFallbackToAnotherTeam() { assertNull(StructurePolicy.selectedTeam(user(UserRole.PADRE,"missing"),"a",teams)) }
    @Test fun preferenceKeysAreAccountScoped() { assertNotEquals(StructurePolicy.selectionKey("one"),StructurePolicy.selectionKey("two"));assertNull(StructurePolicy.selectionKey("")) }
    @Test fun teamRouteContainsFourKnownTabs() { assertEquals(4,StructurePolicy.teamTabs.size);assertEquals("team/a?tab=players",StructurePolicy.team("a","unknown")) }
    @Test fun identifiersCannotInjectRouteSegments() { assertEquals("team/a%2Fb%3Fx?tab=matches",StructurePolicy.team("a/b?x","matches")) }
    @Test fun eventShortcutTargetsEditor() { assertEquals("liveMatch/m?event=true",StructurePolicy.match("m",true)) }
    @Test fun ownAttendanceRequiresBothLinks() { assertNull(StructurePolicy.ownAttendance(user(UserRole.JUGADOR,"a")));assertEquals("attendanceHistory/a/p",StructurePolicy.ownAttendance(user(UserRole.PADRE,"a","p"))) }
    @Test fun playerSearchIsCaseAndAccentInsensitive() { val rows=listOf(Player(id="p",name="Álvaro",number=7));assertEquals(rows,StructurePolicy.players(rows,"alva",null));assertEquals(rows,StructurePolicy.players(rows,"7",null)) }
    @Test fun positionFilterCombinesWithSearch() { val rows=listOf(Player(id="p",name="Alex",position=PlayerPosition.DELANTERO));assertTrue(StructurePolicy.players(rows,"otro",PlayerPosition.DELANTERO).isEmpty());assertEquals(1,StructurePolicy.players(rows,"alex",PlayerPosition.DELANTERO).size) }
    @Test fun matchesCombineQueryAndStatus() { val rows=listOf(LiveMatch(id="x",name="Clásico",status="finished"));assertEquals(1,StructurePolicy.matches(rows,"clasico","finished").size);assertTrue(StructurePolicy.matches(rows,"clasico","in_progress").isEmpty()) }
    @Test fun liveMatchesAppearFirst() { val rows=listOf(LiveMatch(id="old",date=200),LiveMatch(id="live",date=100,status="in_progress"));assertEquals("live",StructurePolicy.matches(rows,"",null).first().id) }
    @Test fun agendaDoesNotInventFutureMatchFromPastOrFinished() { val rows=listOf(LiveMatch(id="past",date=1),LiveMatch(id="done",date=1000,status="finished"));assertNull(StructurePolicy.nextMatch(rows,100)) }
    @Test fun agendaChoosesNearestFutureMatch() { assertEquals("next",StructurePolicy.nextMatch(listOf(LiveMatch(id="later",date=500),LiveMatch(id="next",date=300)),100)?.id) }
    @Test fun trainingAgendaExcludesCancelledAndCompleted() { val rows=listOf(Training(id="x",date=1000,status=TrainingStatus.CANCELADO),Training(id="y",date=1000,status=TrainingStatus.COMPLETADO));assertNull(StructurePolicy.nextTraining(rows,100)) }
    @Test fun liveTrainingTakesPriorityOverFuture() { val rows=listOf(Training(id="future",date=2000),Training(id="now",date=1,status=TrainingStatus.EN_CURSO));assertEquals("now",StructurePolicy.nextTraining(rows,100)?.id) }
}
