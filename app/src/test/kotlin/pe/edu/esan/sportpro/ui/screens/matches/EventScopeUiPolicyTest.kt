package pe.edu.esan.sportpro.ui.screens.matches
import org.junit.Assert.*
import org.junit.Test
import pe.edu.esan.sportpro.data.model.*
class EventScopeUiPolicyTest {
    private val match=LiveMatch(teamA="a",teamB="b")
    @Test fun adminBoth(){assertEquals(listOf("a","b"),eventEditableTeams(User(uid="admin",role=UserRole.ADMIN),match))}
    @Test fun coachOwnOnly(){assertEquals(listOf("b"),eventEditableTeams(User(uid="coach",role=UserRole.ENTRENADOR,coachTeamIds=listOf("b")),match))}
    @Test fun coachNoScope(){assertTrue(eventEditableTeams(User(uid="coach",role=UserRole.ENTRENADOR),match).isEmpty())}
    @Test fun unrelatedScope(){assertTrue(eventEditableTeams(User(uid="coach",role=UserRole.ENTRENADOR,coachTeamIds=listOf("other")),match).isEmpty())}
    @Test fun familyNoWrite(){for(role in listOf(UserRole.JUGADOR,UserRole.PADRE))assertTrue(eventEditableTeams(User(uid="family",role=role,teamId="a",coachTeamIds=listOf("a")),match).isEmpty())}
    @Test fun inactiveNoWrite(){assertTrue(eventEditableTeams(User(uid="admin",role=UserRole.ADMIN,isActive=false),match).isEmpty())}
    @Test fun unknownIdentityNoWrite(){assertTrue(eventEditableTeams(null,match).isEmpty());assertTrue(eventEditableTeams(User(role=UserRole.ADMIN),match).isEmpty());assertTrue(eventEditableTeams(User(uid="admin",role=UserRole.ADMIN),null).isEmpty())}
}
