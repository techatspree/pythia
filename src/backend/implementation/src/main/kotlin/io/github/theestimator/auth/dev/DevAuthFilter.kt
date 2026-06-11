package io.github.theestimator.auth.dev

import io.github.theestimator.auth.CurrentUser
import io.github.theestimator.auth.CurrentUserProvider
import io.quarkus.arc.properties.IfBuildProperty
import jakarta.annotation.Priority
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.Optional

private val SUBJECT_ID_REGEX = Regex("^[a-z][a-z0-9-]*$")

@Provider
@Priority(Priorities.AUTHENTICATION)
@IfBuildProperty(name = "app.auth.provider", stringValue = "dev")
class DevAuthFilter(
    private val currentUserProvider: CurrentUserProvider,
    // SmallRye Config converts unquoted empty strings to null, so use
    // Optional to receive "" as absent and treat that as strict mode.
    @ConfigProperty(name = "app.auth.dev.default-user")
    private val defaultUserConfig: Optional<String>
) : ContainerRequestFilter {

    private val defaultUser: String
        get() = defaultUserConfig.orElse("").trim()

    override fun filter(ctx: ContainerRequestContext) {
        val header = ctx.getHeaderString(HttpHeaders.AUTHORIZATION)
        val user = if (header.isNullOrBlank()) {
            if (defaultUser.isEmpty()) {
                ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build())
                return
            }
            DEV_USERS[defaultUser]
        } else {
            val parts = header.split(Regex("\\s+"), limit = 2)
            if (parts.size != 2 || parts[0] != "Dev") {
                ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build())
                return
            }
            val subjectId = parts[1].trim()
            if (!SUBJECT_ID_REGEX.matches(subjectId)) {
                ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build())
                return
            }
            DEV_USERS[subjectId]
        }

        if (user == null) {
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build())
            return
        }

        currentUserProvider.current = CurrentUser(
            subjectId = user.subjectId,
            email = user.email,
            displayName = user.displayName,
            roles = user.roles,
            providerName = "dev"
        )
    }
}
