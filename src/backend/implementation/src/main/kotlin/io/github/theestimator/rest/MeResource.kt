package io.github.theestimator.rest

import io.github.theestimator.auth.CurrentUserProvider
import io.github.theestimator.auth.Role
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

data class CurrentUserDto(
    val subjectId: String,
    val email: String?,
    val displayName: String?,
    val roles: List<Role>,
    val providerName: String
)

@ApplicationScoped
@Path("/api/auth")
class MeResource(private val currentUserProvider: CurrentUserProvider) {

    @GET
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    fun me(): Response {
        val u = currentUserProvider.current
            ?: return Response.status(Response.Status.UNAUTHORIZED).build()
        return Response.ok(
            CurrentUserDto(
                subjectId = u.subjectId,
                email = u.email,
                displayName = u.displayName,
                roles = u.roles.toList(),
                providerName = u.providerName
            )
        ).build()
    }
}
