package pe.edu.esan.sportpro.data.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.esan.sportpro.data.model.*

interface GuidedAccounts {
    fun register(email: String, password: String, profile: GuidedProfile): Flow<Result<User>>
    suspend fun loadMyData(): GuidedProfile
    suspend fun saveMyData(profile: GuidedProfile)
    suspend fun joinTeam(code: String)
    suspend fun leaveTeam(expectedTeamId: String)
    suspend fun generateTeamCode(teamId: String): String
    suspend fun loadPlayerData(teamId: String, playerId: String): GuidedProfile
    suspend fun savePlayerSports(teamId: String, playerId: String, position: PlayerPosition, dominantFoot: String)
    suspend fun savePlayerAsAdmin(teamId: String, playerId: String, profile: GuidedProfile)
    suspend fun movePlayerAsAdmin(userUid: String, targetTeamId: String)
    suspend fun revokeTeamCode(teamId: String)
    suspend fun loadTeamJoiners(teamId: String): List<TeamJoiner>
    suspend fun removePlayer(teamId: String, playerId: String)
}
