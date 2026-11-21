package pe.edu.esan.sportpro.data.model

import org.junit.Assert.*
import org.junit.Test

class MatchPreparationTest {
    private fun confirmedPreparation(count: Int = 24): MatchPreparation = MatchPreparation(
        teamId = "team",
        revision = 4,
        roster = (1..count).associate { index ->
            val id = "player-$index"
            id to MatchInvitation(id, "Jugador $index", response = Availability.CONFIRMED)
        }
    )

    private fun positionsFor(vararg ids: String): Map<String, String> =
        ids.associateWith { "CENTROCAMPISTA" }

    private fun rejects(action: () -> Unit) {
        assertThrows(IllegalArgumentException::class.java) { action() }
    }

    @Test fun emptyPreparationHasNeitherSavedNorCurrentLineup() {
        val preparation = MatchPreparation()
        assertTrue(preparation.roster.isEmpty())
        assertFalse(preparation.hasLineup)
        assertFalse(preparation.lineupCurrent)
    }

    @Test fun activeInvitationAcceptsBothFinalResponses() {
        val invitation = MatchInvitation("player-1", "Jugador")
        MatchPreparationPolicy.validateResponse(invitation, Availability.CONFIRMED)
        MatchPreparationPolicy.validateResponse(invitation, Availability.DECLINED)
    }

    @Test fun withdrawnOrMissingInvitationCannotRespond() {
        val withdrawn = MatchInvitation("player-1", "Jugador", invited = false)
        for (response in listOf(Availability.CONFIRMED, Availability.DECLINED)) {
            rejects { MatchPreparationPolicy.validateResponse(withdrawn, response) }
            rejects { MatchPreparationPolicy.validateResponse(null, response) }
        }
    }

    @Test fun pendingIsNeverAFinalResponse() {
        for (current in Availability.values()) {
            val invitation = MatchInvitation("player-1", "Jugador", response = current)
            rejects { MatchPreparationPolicy.validateResponse(invitation, Availability.PENDING) }
        }
    }

    @Test fun onlyMatchingRevisionCanBeSaved() {
        MatchPreparationPolicy.checkRevision(actual = 4, expected = 4)
        rejects { MatchPreparationPolicy.checkRevision(actual = 4, expected = 3) }
        rejects { MatchPreparationPolicy.checkRevision(actual = 4, expected = 5) }
    }

    @Test fun invitationLimitCountsWithdrawnEntriesToo() {
        val roster = (1..40).associate { index ->
            val id = "player-$index"
            id to MatchInvitation(id, "Jugador $index", invited = index == 1)
        }
        assertEquals(1, roster.values.count { it.invited })
        MatchPreparationPolicy.validateInvitation("new-player", "Nuevo", roster.size - 1, false)
        rejects {
            MatchPreparationPolicy.validateInvitation("new-player", "Nuevo", roster.size, false)
        }
        MatchPreparationPolicy.validateInvitation("player-40", "Jugador 40", roster.size, true)
    }

    @Test fun invitationRejectsInvalidPlayerIdentifiers() {
        for (id in listOf("", "   ", "players/one", "p".repeat(129))) {
            assertFalse(MatchPreparationPolicy.validId(id))
            rejects { MatchPreparationPolicy.validateInvitation(id, "Jugador", 0, false) }
        }
        val longestAllowed = "p".repeat(128)
        assertTrue(MatchPreparationPolicy.validId(longestAllowed))
        MatchPreparationPolicy.validateInvitation(longestAllowed, "Jugador", 0, false)
    }

    @Test fun invitationNameMustBePresentAndBounded() {
        for (name in listOf("", "   ", "n".repeat(121))) {
            rejects { MatchPreparationPolicy.validateInvitation("player-1", name, 0, false) }
        }
        MatchPreparationPolicy.validateInvitation("player-1", "n".repeat(120), 0, false)
    }

    @Test fun lineupAcceptsOneThroughElevenStartersWithoutRequiringEleven() {
        val preparation = confirmedPreparation()
        for (count in 1..11) {
            val starters = (1..count).map { "player-$it" }
            MatchPreparationPolicy.validateLineup(
                preparation, starters, emptyList(), positionsFor(*starters.toTypedArray())
            )
        }
    }

    @Test fun lineupRejectsZeroAndTwelveStarters() {
        val preparation = confirmedPreparation()
        rejects {
            MatchPreparationPolicy.validateLineup(preparation, emptyList(), emptyList(), emptyMap())
        }
        val tooMany = (1..12).map { "player-$it" }
        rejects {
            MatchPreparationPolicy.validateLineup(
                preparation, tooMany, emptyList(), positionsFor(*tooMany.toTypedArray())
            )
        }
    }

    @Test fun lineupAcceptsZeroThroughTwelveSubstitutes() {
        val preparation = confirmedPreparation()
        for (count in 0..12) {
            val substitutes = (1..count).map { "player-${it + 1}" }
            val all = listOf("player-1") + substitutes
            MatchPreparationPolicy.validateLineup(
                preparation, listOf("player-1"), substitutes, positionsFor(*all.toTypedArray())
            )
        }
    }

    @Test fun lineupRejectsThirteenSubstitutes() {
        val substitutes = (2..14).map { "player-$it" }
        val all = listOf("player-1") + substitutes
        rejects {
            MatchPreparationPolicy.validateLineup(
                confirmedPreparation(), listOf("player-1"), substitutes,
                positionsFor(*all.toTypedArray())
            )
        }
    }

    @Test fun lineupAcceptsElevenStartersAndTwelveDifferentSubstitutesTogether() {
        val starters = (1..11).map { "player-$it" }
        val substitutes = (12..23).map { "player-$it" }
        MatchPreparationPolicy.validateLineup(
            confirmedPreparation(), starters, substitutes,
            positionsFor(*(starters + substitutes).toTypedArray())
        )
    }

    @Test fun duplicateStartersAreRejected() {
        rejects {
            MatchPreparationPolicy.validateLineup(
                confirmedPreparation(), listOf("player-1", "player-1"), emptyList(),
                positionsFor("player-1")
            )
        }
    }

    @Test fun duplicateSubstitutesAreRejected() {
        rejects {
            MatchPreparationPolicy.validateLineup(
                confirmedPreparation(), listOf("player-1"), listOf("player-2", "player-2"),
                positionsFor("player-1", "player-2")
            )
        }
    }

    @Test fun aPlayerCannotBeBothStarterAndSubstitute() {
        rejects {
            MatchPreparationPolicy.validateLineup(
                confirmedPreparation(), listOf("player-1"), listOf("player-1"),
                positionsFor("player-1")
            )
        }
    }

    @Test fun pendingDeclinedAndWithdrawnPlayersCannotEnterEitherLineupRole() {
        val ineligible = listOf(
            MatchInvitation("player-2", "Jugador", response = Availability.PENDING),
            MatchInvitation("player-2", "Jugador", response = Availability.DECLINED),
            MatchInvitation("player-2", "Jugador", invited = false, response = Availability.CONFIRMED)
        )
        for (invitation in ineligible) {
            val preparation = confirmedPreparation().let {
                it.copy(roster = it.roster + ("player-2" to invitation))
            }
            rejects {
                MatchPreparationPolicy.validateLineup(
                    preparation, listOf("player-2"), emptyList(), positionsFor("player-2")
                )
            }
            rejects {
                MatchPreparationPolicy.validateLineup(
                    preparation, listOf("player-1"), listOf("player-2"),
                    positionsFor("player-1", "player-2")
                )
            }
        }
    }

    @Test fun aPlayerAbsentFromRosterCannotEnterEitherLineupRole() {
        val preparation = confirmedPreparation()
        rejects {
            MatchPreparationPolicy.validateLineup(
                preparation, listOf("outsider"), emptyList(), positionsFor("outsider")
            )
        }
        rejects {
            MatchPreparationPolicy.validateLineup(
                preparation, listOf("player-1"), listOf("outsider"),
                positionsFor("player-1", "outsider")
            )
        }
    }

    @Test fun rosterKeyCannotStandInForAnotherPlayersInvitation() {
        val preparation = MatchPreparation(
            roster = mapOf(
                "player-1" to MatchInvitation("other-player", "Otro", response = Availability.CONFIRMED)
            )
        )
        rejects {
            MatchPreparationPolicy.validateLineup(
                preparation, listOf("player-1"), emptyList(), positionsFor("player-1")
            )
        }
    }

    @Test fun invalidPlayerIdsAreRejectedEvenWhenPresentInConfirmedRoster() {
        for (id in listOf("", "players/one", "p".repeat(129))) {
            val preparation = MatchPreparation(
                roster = mapOf(id to MatchInvitation(id, "Jugador", response = Availability.CONFIRMED))
            )
            rejects {
                MatchPreparationPolicy.validateLineup(
                    preparation, listOf(id), emptyList(), positionsFor(id)
                )
            }
        }
    }

    @Test fun positionsMustHaveExactlyTheSelectedPlayerKeys() {
        val preparation = confirmedPreparation()
        val wrongPositions = listOf(
            positionsFor("player-1"),
            positionsFor("player-1", "player-2", "player-3"),
            positionsFor("player-1", "player-3")
        )
        for (selected in wrongPositions) {
            rejects {
                MatchPreparationPolicy.validateLineup(
                    preparation, listOf("player-1"), listOf("player-2"), selected
                )
            }
        }
        MatchPreparationPolicy.validateLineup(
            preparation, listOf("player-1"), listOf("player-2"),
            linkedMapOf("player-2" to "DELANTERO", "player-1" to "PORTERO")
        )
    }

    @Test fun eachEnumeratedPositionIsAcceptedAndOtherValuesAreRejected() {
        val expectedPositions = setOf(
            "PORTERO", "DEFENSA_CENTRAL", "LATERAL_IZQUIERDO", "LATERAL_DERECHO",
            "CENTROCAMPISTA", "EXTREMO", "DELANTERO"
        )
        assertEquals(expectedPositions, MatchPreparationPolicy.positions.toSet())
        for (position in expectedPositions) {
            MatchPreparationPolicy.validateLineup(
                confirmedPreparation(), listOf("player-1"), emptyList(),
                mapOf("player-1" to position)
            )
        }
        for (position in listOf("", "portero", "CAPITAN", " PORTERO ")) {
            rejects {
                MatchPreparationPolicy.validateLineup(
                    confirmedPreparation(), listOf("player-1"), emptyList(),
                    mapOf("player-1" to position)
                )
            }
        }
    }

    @Test fun positionsAreValidatedForSubstitutesToo() {
        rejects {
            MatchPreparationPolicy.validateLineup(
                confirmedPreparation(), listOf("player-1"), listOf("player-2"),
                mapOf("player-1" to "PORTERO", "player-2" to "INVALIDA")
            )
        }
    }

    @Test fun rosterRevisionInvalidatesSavedLineupUntilItIsReviewedAgain() {
        val saved = confirmedPreparation().copy(
            lineupRevision = 4,
            starters = listOf("player-1"),
            positions = positionsFor("player-1")
        )
        assertTrue(saved.hasLineup)
        assertTrue(saved.lineupCurrent)
        val changed = saved.copy(revision = 5)
        assertTrue(changed.hasLineup)
        assertFalse(changed.lineupCurrent)
        rejects {
            MatchPreparationPolicy.checkRevision(actual = changed.revision, expected = saved.revision)
        }
        MatchPreparationPolicy.validateLineup(
            changed, changed.starters, changed.substitutes, changed.positions
        )
        assertTrue(changed.copy(lineupRevision = changed.revision).lineupCurrent)
    }

    @Test fun matchingRevisionNeverMakesAnEmptyLineupCurrent() {
        for (revision in listOf(0L, 1L, 12L)) {
            val empty = MatchPreparation(revision = revision, lineupRevision = revision)
            assertFalse(empty.lineupCurrent)
            assertFalse(empty.copy(substitutes = listOf("player-1")).lineupCurrent)
        }
    }

    @Test fun confirmedInvitationFlowSupportsAMiniMatchWithStarterAndSubstitute() {
        val pending = listOf(
            MatchInvitation("player-1", "Jugador 1"),
            MatchInvitation("player-2", "Jugador 2")
        )
        val roster = pending.mapIndexed { index, invitation ->
            MatchPreparationPolicy.validateInvitation(invitation.playerId, invitation.name, index, false)
            MatchPreparationPolicy.validateResponse(invitation, Availability.CONFIRMED)
            invitation.copy(response = Availability.CONFIRMED, respondedBy = "account-${index + 1}")
        }.associateBy { it.playerId }
        val preparation = MatchPreparation(teamId = "team", revision = 2, roster = roster)
        val starters = listOf("player-1")
        val substitutes = listOf("player-2")
        val selectedPositions = mapOf("player-1" to "PORTERO", "player-2" to "DELANTERO")
        MatchPreparationPolicy.checkRevision(preparation.revision, 2)
        MatchPreparationPolicy.validateLineup(preparation, starters, substitutes, selectedPositions)
        val saved = preparation.copy(
            lineupRevision = preparation.revision,
            starters = starters,
            substitutes = substitutes,
            positions = selectedPositions
        )
        assertTrue(saved.lineupCurrent)
        assertEquals(listOf("player-1"), saved.starters)
        assertEquals(listOf("player-2"), saved.substitutes)
        assertEquals(selectedPositions, saved.positions)
    }
    @Test fun ownCoachCanRegisterWithoutPretendingToBePlayer() {
        val coach = User(uid = "coach", role = UserRole.ENTRENADOR, coachTeamIds = listOf("team"))
        assertTrue(MatchPreparationPolicy.canRecordResponse(coach, "team", "player", true))
        assertFalse(MatchPreparationPolicy.canRecordResponse(coach, "rival", "player", true))
        assertFalse(MatchPreparationPolicy.canRecordResponse(coach.copy(isActive = false), "team", "player", true))
        assertFalse(MatchPreparationPolicy.canRecordResponse(coach, "team", "player", false))
        assertTrue(MatchPreparationPolicy.canRecordResponse(User(uid = "admin", role = UserRole.ADMIN), "team", "player", true))
    }

    @Test fun familyCannotClaimCoachAttributionAndNormalReplyStaysAvailable() {
        val player = User(uid = "player-account", role = UserRole.JUGADOR, teamId = "team", playerId = "player")
        val parent = User(uid = "parent", role = UserRole.PADRE, children = listOf(FamilyChild("team", "player")))
        for (user in listOf(player, parent)) {
            assertTrue(MatchPreparationPolicy.canRecordResponse(user, "team", "player", false))
            assertFalse(MatchPreparationPolicy.canRecordResponse(user, "team", "player", true))
            assertFalse(MatchPreparationPolicy.canRecordResponse(user, "team", "other-player", false))
        }
    }

    @Test fun coachAttributionIsVisibleOnlyForAnActualRecordedResponse() {
        val row = MatchInvitation("player", "Jugador demo", response = Availability.CONFIRMED,
            respondedBy = "coach-uid", recordedByCoach = true)
        assertEquals("coach-uid", row.respondedBy)
        assertEquals("Registrado por el entrenador", MatchPreparationPolicy.responseAttribution(row))
        assertNull(MatchPreparationPolicy.responseAttribution(row.copy(recordedByCoach = false)))
        assertNull(MatchPreparationPolicy.responseAttribution(row.copy(response = Availability.PENDING)))
        assertNull(MatchPreparationPolicy.responseAttribution(row.copy(respondedBy = "")))
    }

    @Test fun aCoachRecordedConfirmationUsesTheSameLineupEligibility() {
        val row = MatchInvitation("player", "Jugador demo", response = Availability.CONFIRMED,
            respondedBy = "coach-uid", recordedByCoach = true)
        val state = MatchPreparation(teamId = "team", revision = 2, roster = mapOf("player" to row))
        MatchPreparationPolicy.validateLineup(state, listOf("player"), emptyList(), mapOf("player" to "DELANTERO"))
        assertThrows(IllegalArgumentException::class.java) {
            MatchPreparationPolicy.validateLineup(state.copy(roster = mapOf("player" to row.copy(response = Availability.DECLINED))),
                listOf("player"), emptyList(), mapOf("player" to "DELANTERO"))
        }
    }

}
