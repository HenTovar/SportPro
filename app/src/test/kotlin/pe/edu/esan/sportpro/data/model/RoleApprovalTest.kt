package pe.edu.esan.sportpro.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RoleApprovalTest {
    @Test fun completeApprovalMatrix() {
        val expected = mapOf<UserRole?, Set<UserRole>>(
            null to emptySet(),
            UserRole.JUGADOR to emptySet(),
            UserRole.PADRE to emptySet(),
            UserRole.ENTRENADOR to setOf(UserRole.JUGADOR, UserRole.PADRE),
            UserRole.ADMIN to setOf(UserRole.JUGADOR, UserRole.PADRE, UserRole.ENTRENADOR)
        )
        for ((actor, allowed) in expected) for (target in UserRole.values()) {
            assertEquals("actor=$actor target=$target", target in allowed,
                RolePolicy.canApprove(actor, false, target))
            assertEquals("verified creator actor=$actor target=$target", true,
                RolePolicy.canApprove(actor, true, target))
        }
    }
}
