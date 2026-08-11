package io.pythia.auth.dev

import io.pythia.auth.Role

data class DevUser(
    val subjectId: String,
    val email: String,
    val displayName: String,
    val roles: Set<Role>
)

val DEV_USERS: Map<String, DevUser> = listOf(
    DevUser("dev-viewer", "viewer@dev.local", "Dev Viewer", setOf(Role.VIEWER)),
    DevUser(
        "dev-estimator",
        "estimator@dev.local",
        "Dev Estimator",
        setOf(Role.VIEWER, Role.ESTIMATOR)
    ),
    DevUser(
        "dev-admin",
        "admin@dev.local",
        "Dev Admin",
        setOf(Role.VIEWER, Role.ESTIMATOR, Role.ADMIN)
    )
).associateBy { it.subjectId }
