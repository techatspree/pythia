package io.pythia.auth

import io.pythia.service.CurrentUserService
import io.pythia.service.preferredLanguage
import io.quarkus.logging.Log
import io.smallrye.common.annotation.Blocking
import jakarta.annotation.Priority
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.ext.Provider

// Provider-agnostic just-in-time user provisioning. Runs after the auth module
// has populated CurrentUserProvider (priority > the auth filters); on an
// authenticated MUTATING request it ensures a persisted User exists, so audit /
// ownership / undo-log FKs resolve for whoever is making changes. `@Blocking`
// so the DB write runs on a worker thread rather than the event loop.
@Provider
@Priority(Priorities.AUTHENTICATION + 200)
@Blocking
class UserProvisioningFilter(
    private val currentUserProvider: CurrentUserProvider,
    private val currentUserService: CurrentUserService
) : ContainerRequestFilter {

    override fun filter(ctx: ContainerRequestContext) {
        if (ctx.method !in MUTATING_METHODS) return
        val current = currentUserProvider.current
        if (current == null) {
            Log.debug(
                "UserProvisioningFilter: ${ctx.method} ${ctx.uriInfo.path} has no authenticated user " +
                    "(no Authorization header?) — skipping provisioning"
            )
            return
        }
        Log.debug("UserProvisioningFilter: ensuring user ${current.subjectId} for ${ctx.method} ${ctx.uriInfo.path}")
        // Seed the language preference from Accept-Language on first sighting;
        // ensureUser ignores it for an already-provisioned user.
        val requestedLanguage = preferredLanguage(ctx.getHeaderString(HttpHeaders.ACCEPT_LANGUAGE))
        currentUserService.ensureUser(current, requestedLanguage)
    }

    private companion object {
        private val MUTATING_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
    }
}
