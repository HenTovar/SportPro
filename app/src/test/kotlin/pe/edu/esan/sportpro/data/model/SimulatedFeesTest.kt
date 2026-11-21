package pe.edu.esan.sportpro.data.model

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

class SimulatedFeesTest {
    private val administrator = User(uid = "administrator", role = UserRole.ADMIN)
    private val player = User(uid = "account", role = UserRole.JUGADOR, teamId = "team-a", playerId = "player-a")
    private val parent = User(uid = "parent", role = UserRole.PADRE,
        children = listOf(FamilyChild("team-a", "player-a"), FamilyChild("team-b", "player-b")))

    private fun rejects(action: () -> Unit) {
        assertThrows(IllegalArgumentException::class.java) { action() }
    }

    @Test fun onlyAnActiveAdministratorCanChangeSimulatedStates() {
        assertTrue(SimulatedFeesPolicy.canManage(administrator))
        for (role in listOf(UserRole.ENTRENADOR, UserRole.PADRE, UserRole.JUGADOR)) {
            assertFalse(SimulatedFeesPolicy.canManage(administrator.copy(role = role)))
        }
        assertFalse(SimulatedFeesPolicy.canManage(administrator.copy(isActive = false)))
        assertFalse(SimulatedFeesPolicy.canManage(administrator.copy(uid = "")))
        assertFalse(SimulatedFeesPolicy.canManage(null))
    }

    @Test fun administratorCanReadAnyValidPlayerPath() {
        assertTrue(SimulatedFeesPolicy.canRead(administrator, "team-a", "player-a"))
        assertTrue(SimulatedFeesPolicy.canRead(administrator, "team-b", "player-b"))
        assertFalse(SimulatedFeesPolicy.canRead(administrator, "", "player-a"))
        assertFalse(SimulatedFeesPolicy.canRead(administrator, "team-a", "../player"))
    }

    @Test fun playerCanReadTheirOwnRecordButNotASiblingOrForeignTeam() {
        assertTrue(SimulatedFeesPolicy.canRead(player, "team-a", "player-a"))
        assertFalse(SimulatedFeesPolicy.canRead(player, "team-a", "player-b"))
        assertFalse(SimulatedFeesPolicy.canRead(player, "team-b", "player-a"))
        assertFalse(SimulatedFeesPolicy.canManage(player))
    }

    @Test fun parentCanReadEachExplicitChildWithoutCrossingTheirPaths() {
        assertTrue(SimulatedFeesPolicy.canRead(parent, "team-a", "player-a"))
        assertTrue(SimulatedFeesPolicy.canRead(parent, "team-b", "player-b"))
        assertFalse(SimulatedFeesPolicy.canRead(parent, "team-a", "player-b"))
        assertFalse(SimulatedFeesPolicy.canRead(parent, "team-b", "player-a"))
        assertFalse(SimulatedFeesPolicy.canManage(parent))
    }

    @Test fun coachCannotReadFinancialRecordsEvenWithTeamScopeOrForgedChildren() {
        val coach = parent.copy(role = UserRole.ENTRENADOR, teamId = "team-a", playerId = "player-a",
            coachTeamIds = listOf("team-a", "team-b"))
        assertFalse(SimulatedFeesPolicy.canRead(coach, "team-a", "player-a"))
        assertFalse(SimulatedFeesPolicy.canRead(coach, "team-b", "player-b"))
        assertFalse(SimulatedFeesPolicy.canManage(coach))
    }

    @Test fun playerCannotExpandAccessByAddingAParentChildrenList() {
        val forged = player.copy(children = listOf(FamilyChild("team-b", "player-b")))
        assertFalse(SimulatedFeesPolicy.canRead(forged, "team-b", "player-b"))
        assertTrue(SimulatedFeesPolicy.canRead(forged, "team-a", "player-a"))
    }

    @Test fun inactiveMissingOrAnonymousProfilesFailClosed() {
        assertFalse(SimulatedFeesPolicy.canRead(null, "team-a", "player-a"))
        assertFalse(SimulatedFeesPolicy.canRead(player.copy(isActive = false), "team-a", "player-a"))
        assertFalse(SimulatedFeesPolicy.canRead(parent.copy(isActive = false), "team-a", "player-a"))
        assertFalse(SimulatedFeesPolicy.canRead(player.copy(uid = ""), "team-a", "player-a"))
        assertFalse(SimulatedFeesPolicy.canRead(administrator.copy(isActive = false), "team-a", "player-a"))
    }

    @Test fun revocationRemovesLegacyGrantWhileFreshExplicitParentGrantStillWorks() {
        val revokedPlayer = player.copy(hasFamilyRevocation = true)
        assertFalse(SimulatedFeesPolicy.canRead(revokedPlayer, "team-a", "player-a"))
        val revokedParent = parent.copy(teamId = "team-old", playerId = "player-old", hasFamilyRevocation = true)
        assertFalse(SimulatedFeesPolicy.canRead(revokedParent, "team-old", "player-old"))
        assertTrue(SimulatedFeesPolicy.canRead(revokedParent, "team-b", "player-b"))
    }

    @Test fun missingRecordIsNotInventedAsAPendingPayment() {
        assertEquals("Sin registro", SimulatedFeesPolicy.label(null))
        assertNotEquals(SimulatedFeesPolicy.label(SimulatedFeeStatus.PENDING), SimulatedFeesPolicy.label(null))
    }

    @Test fun onlyTheThreeSimulatedStatesCanBeParsed() {
        assertEquals(setOf("PENDING", "PAID", "EXEMPT"), SimulatedFeeStatus.values().map { it.name }.toSet())
        for (status in SimulatedFeeStatus.values()) {
            assertEquals(status, SimulatedFeesPolicy.parseStatus(status.name))
            assertTrue(SimulatedFeesPolicy.label(status).contains("simulado"))
        }
        for (value in listOf("", "paid", "PENDING ", "OVERDUE", "REFUNDED", "REAL_PAYMENT")) {
            rejects { SimulatedFeesPolicy.parseStatus(value) }
        }
    }

    @Test fun absentRecordUsesRevisionZeroAndFirstUpdateCannotOverwriteAnExistingRecord() {
        SimulatedFeesPolicy.requireRevision(actual = 0, expected = 0)
        rejects { SimulatedFeesPolicy.requireRevision(actual = 1, expected = 0) }
        rejects { SimulatedFeesPolicy.requireRevision(actual = 0, expected = 1) }
    }

    @Test fun onlyTheRevisionThatWasReviewedCanBeUpdated() {
        SimulatedFeesPolicy.requireRevision(actual = 4, expected = 4)
        rejects { SimulatedFeesPolicy.requireRevision(actual = 4, expected = 3) }
        rejects { SimulatedFeesPolicy.requireRevision(actual = 4, expected = 5) }
        rejects { SimulatedFeesPolicy.requireRevision(actual = -1, expected = -1) }
        rejects { SimulatedFeesPolicy.requireRevision(actual = Long.MAX_VALUE, expected = Long.MAX_VALUE) }
    }

    @Test fun periodMustBeAnExactCalendarMonthInsteadOfAPathOrArbitraryText() {
        for (period in listOf("2026-01", "2026-12", "1000-01", "9999-12")) {
            assertTrue(SimulatedFeesPolicy.validPeriod(period))
            SimulatedFeesPolicy.requirePath("team-a", "player-a", period)
        }
        for (period in listOf("", "2026-00", "2026-13", "2026-1", "2026-001", "0000-01",
            "999-01", "10000-01", "2026/11", "2026-11/extra", " 2026-11", "2026-11\n")) {
            assertFalse(SimulatedFeesPolicy.validPeriod(period))
            rejects { SimulatedFeesPolicy.requirePath("team-a", "player-a", period) }
        }
    }

    @Test fun emptyOversizedAndPathLikeIdentifiersAreRejectedBeforeStorageAccess() {
        for (id in listOf("", "  ", ".", "..", "team/player", "x".repeat(129), "player\n")) {
            assertFalse(SimulatedFeesPolicy.validId(id))
            rejects { SimulatedFeesPolicy.requirePath(id, "player-a", "2026-11") }
            rejects { SimulatedFeesPolicy.requirePath("team-a", id, "2026-11") }
        }
        assertTrue(SimulatedFeesPolicy.validId("x".repeat(128)))
        SimulatedFeesPolicy.requirePath("coach_created-team", "account_player", "2026-11")
    }

    @Test fun monthNavigationHandlesYearRolloverAndLeapFebruary() {
        assertEquals("2027-01", SimulatedFeesPolicy.movePeriod("2026-12", 1))
        assertEquals("2025-12", SimulatedFeesPolicy.movePeriod("2026-01", -1))
        assertEquals("2024-03", SimulatedFeesPolicy.movePeriod("2024-02", 1))
        assertEquals("2024-02", SimulatedFeesPolicy.movePeriod("2024-03", -1))
        assertEquals("2027-11", SimulatedFeesPolicy.movePeriod("2026-11", 12))
        assertEquals("2026-11", SimulatedFeesPolicy.movePeriod("2026-11", 0))
    }

    @Test fun monthNavigationRejectsInvalidInputAndYearOverflow() {
        rejects { SimulatedFeesPolicy.movePeriod("2026-13", 1) }
        rejects { SimulatedFeesPolicy.movePeriod("1000-01", -1) }
        rejects { SimulatedFeesPolicy.movePeriod("9999-12", 1) }
        rejects { SimulatedFeesPolicy.movePeriod("2026-11", Int.MAX_VALUE) }
    }

    @Test fun currentPeriodUsesTheSuppliedCalendarWithoutJavaTimeOrLocaleDigits() {
        val calendar = GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ROOT).apply {
            clear()
            set(2026, Calendar.NOVEMBER, 21, 0, 0, 0)
        }
        assertEquals("2026-11", SimulatedFeesPolicy.currentPeriod(calendar))
    }

    @Test fun schemaHasNoAmountCardOrUnstructuredPaymentFields() {
        assertEquals(setOf("teamId", "playerId", "period", "status", "simulated", "revision",
            "createdBy", "createdAt", "updatedBy", "updatedAt"), SimulatedFeesPolicy.documentKeys)
        for (field in listOf("amount", "currency", "card", "receipt", "notes")) {
            assertFalse(field in SimulatedFeesPolicy.documentKeys)
        }
    }
}
