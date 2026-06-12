package io.github.theestimator.rest

import io.github.theestimator.auth.CurrentUserProvider
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

data class PingDto(val message: String, val user: String)

@ApplicationScoped
@Path("/api/admin")
class AdminPingResource(
    private val currentUserProvider: CurrentUserProvider
) {
    @GET
    @Path("/ping")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    fun ping(): PingDto = PingDto(
        message = "pong",
        user = currentUserProvider.get().subjectId
    )
}
