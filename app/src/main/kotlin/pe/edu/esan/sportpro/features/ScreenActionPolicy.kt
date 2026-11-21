package pe.edu.esan.sportpro.features

import java.net.URLEncoder

/** Presentation destinations only; each screen and the server still authorize its data. */
object ScreenActionPolicy {
    private fun component(id: String) = URLEncoder.encode(id, "UTF-8").replace("+", "%20")

    fun preparationRoute(matchId: String) = "liveMatch/${component(matchId)}?prepare=true"

    fun matchRoute(matchId: String, status: String) = if (status == "not_started")
        preparationRoute(matchId) else "liveMatch/${component(matchId)}?event=false"

    fun matchActionLabel(status: String) = when (status) {
        "not_started" -> "Abrir convocatoria"
        "in_progress" -> "Ver en vivo"
        "finished" -> "Ver resultado"
        else -> "Ver partido"
    }

    fun openPreparation(requested: Boolean, alreadyOpened: Boolean, matchLoaded: Boolean,
                        profileLoaded: Boolean) = requested && !alreadyOpened && matchLoaded && profileLoaded

    const val appearanceRoute = "appearance"
}
