package io.github.theestimator.auth.entra

import io.github.theestimator.auth.Role

fun entraRolesToDomain(claimRoles: Collection<String>): Set<Role> {
    val out = mutableSetOf<Role>()
    for (claim in claimRoles) {
        when (claim.uppercase()) {
            "VIEWER" -> out += Role.VIEWER
            "ESTIMATOR" -> out += Role.ESTIMATOR
            "ADMIN" -> out += Role.ADMIN
        }
    }
    return out
}
