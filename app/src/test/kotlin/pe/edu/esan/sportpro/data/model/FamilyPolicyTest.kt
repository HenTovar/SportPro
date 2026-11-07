package pe.edu.esan.sportpro.data.model

import org.junit.Assert.*
import org.junit.Test

class FamilyPolicyTest {
    private val a = FamilyChild("a", "one", "Uno")
    private val b = FamilyChild("b", "two", "Dos")
    private val parent = User(uid="p", role=UserRole.PADRE, teamId="a", playerId="one", children=listOf(a,b))
    @Test fun legacyFirstDeduplicates() { assertEquals(listOf("a/one","b/two"),FamilyPolicy.children(parent).map{it.key}) }
    @Test fun parentMayReadBothNotStrangers() { assertTrue(RolePolicy.canReadPlayer(parent,"b","two"));assertFalse(RolePolicy.canReadPlayer(parent,"a","two"));assertFalse(RolePolicy.canReadTeam(parent,"c")) }
    @Test fun playerCannotExpandUsingChildren() { val player=parent.copy(role=UserRole.JUGADOR);assertFalse(RolePolicy.canReadPlayer(player,"b","two")) }
    @Test fun inactiveHasNoChildren() { assertTrue(FamilyPolicy.children(parent.copy(isActive=false)).isEmpty()) }
    @Test fun selectionIsFromAuthorizedList() { assertEquals(b,FamilyPolicy.selected(parent,"b/two"));assertEquals("a/one",FamilyPolicy.selected(parent,"foreign")?.key) }
    @Test fun oldAccountWithoutChildListWorks() { assertEquals(1,FamilyPolicy.children(parent.copy(children=emptyList())).size) }
    @Test fun emptyParentCanSelectNothing() { assertNull(FamilyPolicy.selected(User(role=UserRole.PADRE),"")) }
    @Test fun normalizeCodeAndRejectPaths() { assertEquals("ABCDEFGH2345",FamilyPolicy.normalizeCode(" abcd-efgh-2345 "));assertFalse(FamilyPolicy.validCode("../../users"));assertFalse(FamilyPolicy.validCode("")) }
    @Test fun codesHaveSixtyBitsAndNoAmbiguousSymbols() { val codes=(1..1000).map{FamilyPolicy.newCode()};assertTrue(codes.all{FamilyPolicy.validCode(it)});assertEquals(1000,codes.distinct().size) }
    @Test fun decodeNestedMap() { assertEquals(listOf(a),FamilyPolicy.decode(mapOf("a" to mapOf("one" to mapOf("name" to "Uno","code" to "abc"))))) }
    @Test fun malformedMapsCannotAddArbitraryLinks() { assertTrue(FamilyPolicy.decode(listOf(a)).isEmpty());assertTrue(FamilyPolicy.decode(mapOf("a" to "bad")).isEmpty()) }
    @Test fun actualFirestoreMapperKeepsFamilyMapOutOfRegistration() {
        val plain=com.google.firebase.firestore.util.CustomClassMapper.convertToPlainJavaTypes(parent) as Map<*,*>
        assertFalse(plain.containsKey("children"))
        assertFalse(plain.containsKey("coachTeamIds"))
        assertFalse(plain.containsKey("hasFamilyRevocation"))
        assertTrue(plain.containsKey("active"))
    }
    @Test fun actualFirestoreMapperAcceptsProfileWithNestedChildrenAndUnknownFields() {
        val raw=mapOf("uid" to "p", "role" to "PADRE", "active" to true,
            "children" to mapOf("b" to mapOf("two" to mapOf("name" to "Dos", "code" to "ABCDEFGH2345"))),
            "lastFamilyCode" to "ABCDEFGH2345")
        val account=com.google.firebase.firestore.util.CustomClassMapper.convertToCustomClass(raw, User::class.java, null)
        assertEquals(UserRole.PADRE,account.role)
        assertTrue(account.children.isEmpty()) // Excluded derived field; repository decodes the persisted map explicitly.
        assertEquals(listOf(b),FamilyPolicy.decode(raw["children"]))
    }
}
