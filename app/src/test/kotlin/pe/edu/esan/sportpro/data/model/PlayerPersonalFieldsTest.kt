package pe.edu.esan.sportpro.data.model
import org.junit.Test
import org.junit.Assert.*
class PlayerPersonalFieldsTest {
    @Test fun authorizedProjectionShowsContactsWithoutTouchingSports() {
        val old=Player(id="fixture",position=PlayerPosition.PORTERO,userUid="fixture")
        val p=PlayerPersonalFields.apply(old,mapOf("phone" to "0000000","heightCm" to 170.5,"weightKg" to 65.0,"dateOfBirth" to 946684800000L))
        assertEquals("0000000",p.phone);assertEquals(170.5f,p.height);assertEquals(65f,p.weight);assertEquals(old.position,p.position);assertEquals(old.userUid,p.userUid)
    }
    @Test fun deniedOrOfflineContactReadNeverFallsBackToStalePrivateData() {
        val p=PlayerPersonalFields.apply(Player(phone="STALE",height=170f,weight=65f,dateOfBirth=1L,emergencyContactName="STALE"),null)
        assertEquals("",p.phone);assertEquals(0L,p.dateOfBirth);assertEquals(0f,p.height);assertEquals("",p.emergencyContactName)
    }
}
