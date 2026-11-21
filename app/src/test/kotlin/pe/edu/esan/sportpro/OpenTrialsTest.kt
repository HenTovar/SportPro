package pe.edu.esan.sportpro

import pe.edu.esan.sportpro.data.model.*

import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class OpenTrialsTest {
    private fun day(value: String) = requireNotNull(OpenTrialDates.parse(value))
    private fun draft(position: TrialPosition = TrialPosition.ANY, date: String = "2026-12-01",
        location: String = "Estadio San Martín", category: TeamCategory = TeamCategory.SUB_15) =
        OpenTrialDraft("demo-team","Prueba abierta institucional",category,position,day(date),location,
            "Jornada deportiva organizada por el equipo.")
    private fun trial(id: String = "demo", draft: OpenTrialDraft = draft(), state: TrialState = TrialState.PENDING) =
        OpenTrial(id,draft,state,revision=3)
    private val admin = User(uid="demo-admin",role=UserRole.ADMIN)
    private val coach = User(uid="demo-coach",role=UserRole.ENTRENADOR,coachTeamIds=listOf("demo-team"))

    @Test fun strictDatesRejectImpossibleAndTrailingInput() {
        for(value in listOf("2026-02-29","2026-04-31","2026-13-01","2026-00-10","2026-01-00",
            "2026-1-01","2026-01-1","2026-01-01 ","2026-01-01junk","01/12/2026","1969-12-31",""))
            assertNull(value,OpenTrialDates.parse(value))
    }
    @Test fun leapCenturyRulesAreStrict() {
        assertNotNull(OpenTrialDates.parse("2024-02-29"))
        assertNotNull(OpenTrialDates.parse("2000-02-29"))
        assertNull(OpenTrialDates.parse("2100-02-29"))
        assertNull(OpenTrialDates.parse("1900-02-29"))
    }
    @Test fun datesRemainUtcMidnightAcrossDeviceTimeZones() {
        val original=TimeZone.getDefault()
        try {
            for(zone in listOf("America/Lima","Pacific/Auckland","America/New_York")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                assertEquals(0L,day("1970-01-01"))
                assertEquals(0L,day("2026-12-01") % OpenTrialDates.DAY_MILLIS)
                assertEquals("2026-12-01",OpenTrialDates.format(day("2026-12-01")))
            }
        } finally { TimeZone.setDefault(original) }
    }
    @Test fun filtersHaveVisibleInvalidAndReversedDateErrors() {
        assertNotNull(OpenTrialFilters(from="2026-02-30").validate())
        assertNotNull(OpenTrialFilters(to="invalid").validate())
        val reversed=OpenTrialFilters(from="2026-12-03",to="2026-12-01")
        assertNotNull(reversed.validate())
        assertThrows(IllegalArgumentException::class.java) { OpenTrialsPolicy.filter(emptyList(),reversed) }
        assertNull(OpenTrialFilters(from="2026-12-01",to="2026-12-01").validate())
    }
    @Test fun dateBoundariesAreInclusive() {
        val rows=(1..3).map { trial("demo-$it",draft(date="2026-12-0$it")) }
        assertEquals(listOf("demo-1","demo-2"),OpenTrialsPolicy.filter(rows,
            OpenTrialFilters(from="2026-12-01",to="2026-12-02")).map { it.id })
        assertEquals(listOf("demo-2"),OpenTrialsPolicy.filter(rows,
            OpenTrialFilters(from="2026-12-02",to="2026-12-02")).map { it.id })
    }
    @Test fun anyOfferedPositionMatchesSpecificSearchAndAnySearchIncludesAll() {
        val rows=listOf(trial("any"),trial("keeper",draft(TrialPosition.PORTERO)),trial("forward",draft(TrialPosition.DELANTERO)))
        assertEquals(setOf("any","keeper"),OpenTrialsPolicy.filter(rows,
            OpenTrialFilters(position=TrialPosition.PORTERO)).map { it.id }.toSet())
        assertEquals(3,OpenTrialsPolicy.filter(rows,OpenTrialFilters()).size)
    }
    @Test fun categoryAndAccentInsensitiveLocationFiltersCompose() {
        val rows=listOf(trial("wanted"),trial("wrong-category",draft(category=TeamCategory.SENIOR)),
            trial("wrong-place",draft(location="Campo Central")))
        val actual=OpenTrialsPolicy.filter(rows,OpenTrialFilters(category=TeamCategory.SUB_15,location="  SAN MARTIN "))
        assertEquals(listOf("wanted"),actual.map { it.id })
    }
    @Test fun normalizationDoesNotDependOnTurkishDeviceLocale() {
        val original=Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("san martin",OpenTrialsPolicy.normalize("SAN MARTÍN"))
        } finally { Locale.setDefault(original) }
    }
    @Test fun matchingResultBeyondFirstFiftyIsNeverDropped() {
        val rows=(1..80).map { trial("demo-$it",draft(location=if(it==80) "Campo buscado" else "Otro campo")) }
        assertEquals(listOf("demo-80"),OpenTrialsPolicy.filter(rows,OpenTrialFilters(location="buscado")).map { it.id })
    }
    @Test fun allTeamCategoriesAndPlayerPositionsRemainAvailable() {
        assertEquals(PlayerPosition.entries.map { it.name }.toSet(),TrialPosition.entries.filter { it!=TrialPosition.ANY }.map { it.name }.toSet())
        TeamCategory.entries.forEach { assertNull(OpenTrialsPolicy.validate(draft(category=it))) }
    }
    @Test fun staffScopeIsRequiredForProposalAndRevision() {
        assertTrue(OpenTrialsPolicy.canPropose(coach,"demo-team"))
        assertFalse(OpenTrialsPolicy.canPropose(coach,"other-team"))
        assertTrue(OpenTrialsPolicy.canPropose(admin,"other-team"))
        for(role in listOf(UserRole.JUGADOR,UserRole.PADRE))
            assertFalse(OpenTrialsPolicy.canPropose(User(uid="demo",role=role,teamId="demo-team"),"demo-team"))
        assertFalse(OpenTrialsPolicy.canPropose(coach.copy(isActive=false),"demo-team"))
        assertFalse(OpenTrialsPolicy.canPropose(coach.copy(uid=""),"demo-team"))
        assertTrue(OpenTrialsPolicy.canRevise(coach,trial(state=TrialState.PUBLISHED)))
    }
    @Test fun onlyActiveAdminCanModeratePending() {
        val pending=trial()
        assertEquals(TrialState.PUBLISHED,OpenTrialsPolicy.moderationTarget(admin,pending,3,true))
        assertEquals(TrialState.REJECTED,OpenTrialsPolicy.moderationTarget(admin,pending,3,false))
        assertFalse(OpenTrialsPolicy.canModerate(coach,pending))
        assertFalse(OpenTrialsPolicy.canModerate(admin.copy(isActive=false),pending))
        assertFalse(OpenTrialsPolicy.canModerate(admin,trial(state=TrialState.PUBLISHED)))
        assertFalse(OpenTrialsPolicy.canModerate(admin,trial(state=TrialState.REJECTED)))
        assertThrows(IllegalArgumentException::class.java) { OpenTrialsPolicy.moderationTarget(coach,pending,3,true) }
    }
    @Test fun staleModerationRevisionFails() {
        assertThrows(IllegalArgumentException::class.java) { OpenTrialsPolicy.moderationTarget(admin,trial(),2,true) }
    }
    @Test fun publicTextHasBoundsAndRejectsContacts() {
        assertNull(OpenTrialsPolicy.validate(draft()))
        for(text in listOf(""," ","x".repeat(121),"Correo demo@example.test","https://example.test", "www.example.test", "+51 999 888 777"))
            assertNotNull(text,OpenTrialsPolicy.validate(draft().copy(title=text)))
        assertNotNull(OpenTrialsPolicy.validate(draft().copy(location="x".repeat(161))))
        assertNotNull(OpenTrialsPolicy.validate(draft().copy(description="x".repeat(1501))))
        assertNotNull(OpenTrialsPolicy.validate(draft().copy(dateMillis=day("2026-12-01")+1)))
        assertNotNull(OpenTrialsPolicy.validate(draft().copy(dateMillis=-1)))
        assertNotNull(OpenTrialsPolicy.validate(draft().copy(teamId="")))
    }
}
