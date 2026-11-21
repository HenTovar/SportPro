package pe.edu.esan.sportpro.features

import org.junit.Assert.*
import org.junit.Test
import pe.edu.esan.sportpro.data.model.*

class UiInteractionPolicyTest {
    @Test fun onlyScheduledMatchOffersStart() {
        assertTrue(UiInteractionPolicy.canStart("not_started"))
        for (status in listOf("in_progress", "finished", "", "unknown")) assertFalse(UiInteractionPolicy.canStart(status))
    }
    @Test fun onlyLiveMatchOffersFinish() {
        assertTrue(UiInteractionPolicy.canFinish("in_progress"))
        for (status in listOf("not_started", "finished", "", "unknown")) assertFalse(UiInteractionPolicy.canFinish(status))
    }
    @Test fun fullStarterListCannotAddButCanRemove() {
        assertFalse(UiInteractionPolicy.canChooseSlot(false, 11, 11))
        assertTrue(UiInteractionPolicy.canChooseSlot(true, 11, 11))
    }
    @Test fun fullSubstituteListCannotAddButCanRemove() {
        assertFalse(UiInteractionPolicy.canChooseSlot(false, 12, 12))
        assertTrue(UiInteractionPolicy.canChooseSlot(true, 12, 12))
    }
    @Test fun underLimitCanAdd() {
        assertTrue(UiInteractionPolicy.canChooseSlot(false, 10, 11))
        assertTrue(UiInteractionPolicy.canChooseSlot(false, 11, 12))
    }
    @Test fun malformedCountsDoNotEnableActions() {
        assertFalse(UiInteractionPolicy.canChooseSlot(false, -1, 11))
        assertFalse(UiInteractionPolicy.canChooseSlot(true, 0, 0))
    }
    @Test fun pressingPresentDoesNotMarkAbsent() {
        assertNull(UiInteractionPolicy.attendanceChoice(true, true, true))
        assertEquals(true, UiInteractionPolicy.attendanceChoice(false, true, true))
    }
    @Test fun pressingAbsentIsExplicitAndIdempotent() {
        assertEquals(false, UiInteractionPolicy.attendanceChoice(true, true, false))
        assertNull(UiInteractionPolicy.attendanceChoice(false, true, false))
    }
    @Test fun unregisteredFalseIsNotAlreadyAnAbsence() {
        assertEquals(false, UiInteractionPolicy.attendanceChoice(false, false, false))
    }
    @Test fun injuriesAndExcusedAreNotShownAsAbsence() {
        for (status in listOf(null, AttendanceStatus.LESION, AttendanceStatus.EXCUSED))
            assertFalse(UiInteractionPolicy.hasAttendanceChoice(status, false))
        assertTrue(UiInteractionPolicy.hasAttendanceChoice(AttendanceStatus.LESION, true))
    }
    @Test fun persistedPresenceAndAbsenceAreChoices() {
        assertTrue(UiInteractionPolicy.hasAttendanceChoice(AttendanceStatus.ASISTIO, false))
        assertTrue(UiInteractionPolicy.hasAttendanceChoice(AttendanceStatus.FALTA, false))
    }
    @Test fun remoteLibraryReadKeepsExistingStaffAudience() {
        for (role in listOf(UserRole.ADMIN, UserRole.ENTRENADOR))
            assertTrue(UiInteractionPolicy.canReadExerciseLibrary(User(uid="staff", role=role)))
        for (role in listOf(UserRole.JUGADOR, UserRole.PADRE))
            assertFalse(UiInteractionPolicy.canReadExerciseLibrary(User(uid="member", role=role)))
    }
    @Test fun inactiveAndMissingAccountDoNotQueryLibrary() {
        assertFalse(UiInteractionPolicy.canReadExerciseLibrary(null))
        assertFalse(UiInteractionPolicy.canReadExerciseLibrary(User(role=UserRole.ADMIN)))
        assertFalse(UiInteractionPolicy.canReadExerciseLibrary(User(role=UserRole.ADMIN, isActive=false)))
    }
    @Test fun knownExerciseShowsNameAndDurationInsteadOfId() {
        val rows=mapOf("private-id" to Exercise(id="private-id",name="Pase en triángulo",duration=12))
        assertEquals("Pase en triángulo · 12 min",UiInteractionPolicy.exerciseLabel("private-id",rows))
    }
    @Test fun missingExerciseDoesNotLeakAnInternalIdentifier() {
        val label=UiInteractionPolicy.exerciseLabel("opaque-document-id",emptyMap())
        assertEquals("Ejercicio no disponible",label)
        assertFalse(label.contains("opaque-document-id"))
    }
    @Test fun blankNameIsUnavailableRatherThanEmpty() {
        assertEquals("Ejercicio no disponible",UiInteractionPolicy.exerciseLabel("x",mapOf("x" to Exercise(id="x"))))
    }
    @Test fun catalogResolvesStockIdsLikeTheExistingSelector() {
        val stock=ExerciseLibrary.defaultExercises.first()
        val rows=UiInteractionPolicy.exerciseCatalog(listOf(stock.copy(name="Different"),Exercise(id="custom",name="Custom")))
        assertEquals(stock.name,rows.getValue(stock.id).name)
        assertEquals("Custom",rows.getValue("custom").name)
    }
    @Test fun everyCategoryHasALegibleLabel() {
        assertEquals("Sub-15",UiInteractionPolicy.categoryLabel(TeamCategory.SUB_15))
        assertEquals("Sénior",UiInteractionPolicy.categoryLabel(TeamCategory.SENIOR))
        for (category in TeamCategory.values()) assertFalse(UiInteractionPolicy.categoryLabel(category).contains('_'))
    }
    @Test fun positionLabelsAreHumanReadable() {
        val stored = "DEFENSA_CENTRAL"
        assertEquals("Defensa central",UiInteractionPolicy.positionLabel(stored))
        for (position in MatchPreparationPolicy.positions)
            assertFalse(UiInteractionPolicy.positionLabel(position).contains('_'))
    }
}
