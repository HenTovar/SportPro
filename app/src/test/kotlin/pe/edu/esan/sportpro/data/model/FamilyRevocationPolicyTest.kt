package pe.edu.esan.sportpro.data.model
import org.junit.Assert.*
import org.junit.Test

class FamilyRevocationPolicyTest {
    private val one=mapOf("name" to "Uno","code" to "A")
    private val two=mapOf("name" to "Dos","code" to "B")
    @Test fun removesOnlyChosenSibling() {
        val original=mapOf("a" to mapOf("one" to one,"two" to two),"b" to mapOf("other" to two))
        assertEquals(mapOf("a" to mapOf("two" to two),"b" to mapOf("other" to two)),FamilyPolicy.withoutChild(original,"a","one"))
        assertEquals(2,original["a"]?.size)
    }
    @Test fun dropsEmptyTeamButNotOtherTeams() {
        assertEquals(mapOf("b" to mapOf("two" to two)),FamilyPolicy.withoutChild(mapOf("a" to mapOf("one" to one),"b" to mapOf("two" to two)),"a","one"))
    }
    @Test fun lastChildLeavesEmptyMap() { assertTrue(FamilyPolicy.withoutChild(mapOf("a" to mapOf("one" to one)),"a","one").isEmpty()) }
    @Test fun legacyWithoutMapWorks() { assertTrue(FamilyPolicy.withoutChild(null,"a","one").isEmpty()) }
    @Test fun absentTargetCannotRemoveOthers() {
        val original=mapOf("a" to mapOf("one" to one))
        assertEquals(original,FamilyPolicy.withoutChild(original,"a","foreign"))
        assertEquals(original,FamilyPolicy.withoutChild(original,"foreign","one"))
    }
    @Test fun playerMapperRetainsRevocationStampWhenEditing() {
        val stamp=com.google.firebase.Timestamp(100,0)
        val player=com.google.firebase.firestore.util.CustomClassMapper.convertToCustomClass(
            mapOf("id" to "one", "name" to "Uno", "familyRevokedAt" to stamp, "familyRevokedParentUid" to "old-parent"),Player::class.java,null)
        val edited=player.copy(name="Actualizado")
        val data=com.google.firebase.firestore.util.CustomClassMapper.convertToPlainJavaTypes(edited) as Map<*,*>
        assertEquals(stamp,data["familyRevokedAt"]); assertEquals("old-parent",data["familyRevokedParentUid"])
    }
}
