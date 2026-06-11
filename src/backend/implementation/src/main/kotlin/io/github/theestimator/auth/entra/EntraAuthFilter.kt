package io.github.theestimator.auth.entra

import io.github.theestimator.auth.CurrentUser
import io.github.theestimator.auth.CurrentUserProvider
import io.quarkus.arc.properties.IfBuildProperty
import io.quarkus.security.identity.SecurityIdentity
import jakarta.annotation.Priority
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.ext.Provider
import org.eclipse.microprofile.jwt.JsonWebToken

@Provider
@Priority(Priorities.AUTHENTICATION + 100)
@IfBuildProperty(name = "app.auth.provider", stringValue = "entra")
class EntraAuthFilter(
    private val currentUserProvider: CurrentUserProvider,
    private val identity: SecurityIdentity,
    private val jwt: JsonWebToken
) : ContainerRequestFilter {

    override fun filter(ctx: ContainerRequestContext) {
        if (identity.isAnonymous) return

        val subjectId = identity.principal?.name?.takeIf { it.isNotBlank() } ?: return
        val email: String? = jwt.getClaim<String?>("email")
            ?: jwt.getClaim<String?>("preferred_username")
        val displayName: String? = jwt.getClaim<String?>("name")
        val claimRoles: Collection<String> =
            jwt.getClaim<Collection<String>?>("roles") ?: emptyList()

        currentUserProvider.current = CurrentUser(
            subjectId = subjectId,
            email = email,
            displayName = displayName,
            roles = entraRolesToDomain(claimRoles),
            providerName = "entra"
        )
    }
}
