package pe.edu.esan.sportpro.data.model

import org.junit.Assert.*
import org.junit.Test

class GuidedProfileTest {
    private fun player()=GuidedProfile(name="Adulto ficticio",phone="0000000",dateOfBirth=946684800000,
        position=PlayerPosition.DELANTERO,dominantFoot="RIGHT")
    @Test fun registrationRequiresPlayerDetails() { player().validate() }
    @Test fun heightAndWeightOptional() { player().copy(heightCm=null,weightKg=null).validate() }
    @Test fun coachCanHaveZeroYears() { GuidedProfile(requestedRole=UserRole.ENTRENADOR,name="Adulto ficticio",phone="0000000",specialtyOrLicense="Licencia",yearsExperience=0).validate() }
    @Test fun parentNeedsNoChildOrRelation() { GuidedProfile(requestedRole=UserRole.PADRE,name="Tutor ficticio",phone="0000000").validate() }
    @Test fun missingFieldsRejected() {
        for(p in listOf(player().copy(name=" "),player().copy(phone=""),player().copy(dateOfBirth=null),player().copy(position=null),player().copy(dominantFoot="BAD"))) {
            try {p.validate();fail("accepted incomplete registration")} catch(_:IllegalArgumentException) { }
        }
    }
    @Test fun nonFiniteAndOutOfBoundsRejected() {
        for(p in listOf(player().copy(heightCm=Double.NaN),player().copy(weightKg=Double.POSITIVE_INFINITY),player().copy(heightCm=29.0),player().copy(weightKg=501.0))) {
            try {p.validate();fail("accepted invalid measurement")} catch(_:IllegalArgumentException) { }
        }
    }
    @Test fun invalidExperienceRejected() {
        for(years in listOf(-1,81)) try { GuidedProfile(requestedRole=UserRole.ENTRENADOR,name="Adulto",phone="0000000",specialtyOrLicense="Licencia",yearsExperience=years).validate();fail("accepted experience") } catch(_:IllegalArgumentException) { }
    }
    @Test fun legacyPersonalEditDoesNotRequireSports() { player().copy(position=null,dominantFoot=null).validate(requireSports=false) }
    @Test fun pendingRoleIsNotAuthorization() { assertEquals(UserRole.JUGADOR,RolePolicy.registrationBaseRole(UserRole.ENTRENADOR));assertFalse(RolePolicy.isStaff(RolePolicy.registrationBaseRole(UserRole.ENTRENADOR))) }
    @Test fun registrationNeverGrantsAdmin() { try {RolePolicy.registrationBaseRole(UserRole.ADMIN);fail("self admin")}catch(_:IllegalArgumentException) { } }
}
