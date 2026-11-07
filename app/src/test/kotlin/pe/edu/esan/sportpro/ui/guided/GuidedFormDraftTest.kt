package pe.edu.esan.sportpro.ui.guided

import org.junit.Assert.*
import org.junit.Test
import pe.edu.esan.sportpro.data.model.PlayerPosition
import pe.edu.esan.sportpro.data.model.UserRole

class GuidedFormDraftTest {
    private val now = 1_800_000_000_000L
    private fun player() = GuidedFormDraft(UserRole.JUGADOR, "Persona Ficticia", "999111222", "2000-02-29",
        position = PlayerPosition.DELANTERO, foot = FootChoice.RIGHT)
    @Test fun playerRequiresEverySpecifiedFieldExceptHeightAndWeight() {
        val p = player();assertTrue(p.errors(FormAccess.REGISTRATION, now).isEmpty())
        assertTrue(ProfileField.NAME in p.copy(name = "").errors(FormAccess.REGISTRATION, now))
        assertTrue(ProfileField.PHONE in p.copy(phone = "").errors(FormAccess.REGISTRATION, now))
        assertTrue(ProfileField.BIRTH_DATE in p.copy(birthDate = "").errors(FormAccess.REGISTRATION, now))
        assertTrue(ProfileField.POSITION in p.copy(position = null).errors(FormAccess.REGISTRATION, now))
        assertTrue(ProfileField.FOOT in p.copy(foot = null).errors(FormAccess.REGISTRATION, now))
    }
    @Test fun parentDoesNotNeedPlayerOrCoachFields() {
        assertTrue(GuidedFormDraft(UserRole.PADRE, "Tutor Ficticio", "999111222").errors(FormAccess.REGISTRATION, now).isEmpty())
    }
    @Test fun coachNeedsSpecialtyAndExperienceAndZeroIsValid() {
        val p = GuidedFormDraft(UserRole.ENTRENADOR, "Persona Ficticia", "999111222")
        assertEquals(setOf(ProfileField.SPECIALTY, ProfileField.EXPERIENCE), p.errors(FormAccess.REGISTRATION, now).keys)
        assertTrue(p.copy(specialtyOrLicense = "Entrenamiento", yearsExperience = "0").errors(FormAccess.REGISTRATION, now).isEmpty())
        assertTrue(ProfileField.EXPERIENCE in p.copy(specialtyOrLicense = "Licencia", yearsExperience = "-1").errors(FormAccess.REGISTRATION, now))
    }
    @Test fun roleSwitchClearsHiddenRoleSpecificData() {
        val next = player().copy(heightCm = "170", weightKg = "65").selectRole(UserRole.PADRE)
        assertEquals("Persona Ficticia", next.name);assertEquals("999111222", next.phone)
        assertEquals("", next.birthDate);assertEquals("", next.heightCm);assertNull(next.position);assertNull(next.foot)
    }
    @Test fun noRoleOrAdminSignupIsRejected() {
        assertTrue(ProfileField.ROLE in GuidedFormDraft().errors(FormAccess.REGISTRATION, now))
        assertTrue(ProfileField.ROLE in player().copy(role = UserRole.ADMIN).errors(FormAccess.REGISTRATION, now))
    }
    @Test fun strictDatesRejectInvalidLeapDayAndFuture() {
        assertNotNull(GuidedFormDraft.parseBirthDate("2000-02-29"))
        listOf("2001-02-29", "2000-13-01", "2000-02-300", "2000-2-29", "").forEach { assertNull(GuidedFormDraft.parseBirthDate(it)) }
        assertTrue(ProfileField.BIRTH_DATE in player().copy(birthDate = "2099-01-01").errors(FormAccess.REGISTRATION, now))
    }
    @Test fun optionalPhysicalNumbersRejectNonfiniteAndNonpositive() {
        assertTrue(player().errors(FormAccess.REGISTRATION, now).isEmpty())
        assertTrue(player().copy(heightCm = "170.5", weightKg = "65").errors(FormAccess.REGISTRATION, now).isEmpty())
        listOf("NaN", "Infinity", "1e309", "-1", "0", "wrong").forEach {
            assertTrue(it, ProfileField.HEIGHT in player().copy(heightCm = it).errors(FormAccess.REGISTRATION, now))
            assertTrue(it, ProfileField.WEIGHT in player().copy(weightKg = it).errors(FormAccess.REGISTRATION, now))
        }
    }
    @Test fun numericLimitsMatchTheAgreedRepositoryContract() {
        assertTrue(player().copy(heightCm = "30", weightKg = "500").errors(FormAccess.REGISTRATION, now).isEmpty())
        assertTrue(ProfileField.HEIGHT in player().copy(heightCm = "261").errors(FormAccess.REGISTRATION, now))
        assertTrue(ProfileField.WEIGHT in player().copy(weightKg = "501").errors(FormAccess.REGISTRATION, now))
        val coach = GuidedFormDraft(UserRole.ENTRENADOR, "Persona Ficticia", "999111222", specialtyOrLicense = "Licencia", yearsExperience = "81")
        assertTrue(ProfileField.EXPERIENCE in coach.errors(FormAccess.REGISTRATION, now))
    }
    @Test fun selfCannotEditSportsAndCoachCannotEditPersonal() {
        val p = player()
        assertFalse(p.canEdit(ProfileField.POSITION, FormAccess.SELF));assertFalse(p.canEdit(ProfileField.FOOT, FormAccess.SELF))
        assertTrue(p.canEdit(ProfileField.PHONE, FormAccess.SELF))
        assertTrue(p.canEdit(ProfileField.POSITION, FormAccess.STAFF_SPORTS))
        assertFalse(p.canEdit(ProfileField.NAME, FormAccess.STAFF_SPORTS));assertFalse(p.canEdit(ProfileField.PHONE, FormAccess.STAFF_SPORTS))
        assertTrue(p.canEdit(ProfileField.PHONE, FormAccess.ADMIN));assertFalse(p.canEdit(ProfileField.ROLE, FormAccess.ADMIN))
    }
    @Test fun readOnlyLegacySportsDoNotBlockPersonalCorrections() {
        assertTrue(player().copy(position = null, foot = null).errors(FormAccess.SELF, now).isEmpty())
    }
    @Test fun sportsEditorValidatesOnlyEditableSports() {
        val p = GuidedFormDraft(UserRole.JUGADOR, position = PlayerPosition.PORTERO, foot = FootChoice.BOTH)
        assertTrue(p.errors(FormAccess.STAFF_SPORTS, now).isEmpty())
        assertTrue(ProfileField.FOOT in p.copy(foot = null).errors(FormAccess.STAFF_SPORTS, now))
    }
}
