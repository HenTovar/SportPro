package pe.edu.esan.sportpro.features

/** The next account is never admitted until every cleanup step succeeds. */
interface AccountExitSteps {
    suspend fun markPending()
    suspend fun detachScreens()
    suspend fun signOut()
    suspend fun clearSelectionsAndNotifications()
    suspend fun terminateAndClearCache()
    suspend fun finish()
}

object AccountExit {
    suspend fun run(steps: AccountExitSteps) {
        steps.markPending()
        steps.detachScreens()
        steps.signOut()
        steps.clearSelectionsAndNotifications()
        steps.terminateAndClearCache()
        steps.finish()
    }
}
