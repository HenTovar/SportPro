package pe.edu.esan.sportpro.ui.screens.home

import pe.edu.esan.sportpro.data.model.RolePolicy
import pe.edu.esan.sportpro.data.model.UserRole

/** Presentation only: requesting a role never grants it. */
object ProfileRoleUi {
    val initialTarget: UserRole? = null

    fun roleText(role: UserRole?, loading: Boolean, loadError: Boolean, creator: Boolean = false): String =
        "Rol: " + when {
            role == null && loading && !loadError -> "Cargando…"
            role == null -> "No disponible"
            creator -> "Creador"
            else -> RolePolicy.label(role)
        }

    fun canRequest(role: UserRole?, target: UserRole?, loading: Boolean, loadError: Boolean,
                   busy: Boolean, requested: Boolean) =
        role != null && target != null && target != role && !loading && !loadError && !busy && !requested
}
