package io.github.theestimator.auth.dev

import io.github.theestimator.auth.CurrentUser
import io.github.theestimator.auth.CurrentUserProvider
import io.quarkus.arc.properties.IfBuildProperty
import io.quarkus.security.identity.SecurityIdentity
import jakarta.annotation.Priority
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.ext.Provider

@Provider
@Priority(Priorities.AUTHENTICATION + 100)
@IfBuildProperty(name = "app.auth.provider", stringValue = "dev")
class DevAuthFilter(
    private val currentUserProvider: CurrentUserProvider,
    private val identity: SecurityIdentity
) : ContainerRequestFilter {

    override fun filter(ctx: ContainerRequestContext) {
        if (identity.isAnonymous) return
        val subjectId = identity.principal?.name?.takeIf { it.isNotBlank() } ?: return
        val user = DEV_USERS[subjectId] ?: return
        currentUserProvider.current = CurrentUser(
            subjectId = user.subjectId,
            email = user.email,
            displayName = user.displayName,
            roles = user.roles,
            providerName = "dev"
        )
    }
}
