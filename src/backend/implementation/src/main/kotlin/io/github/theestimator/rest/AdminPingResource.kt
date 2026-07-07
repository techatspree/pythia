package io.github.theestimator.rest

import io.github.theestimator.auth.CurrentUserProvider
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.tags.Tag

data class PingDto(val message: String, val user: String)

@ApplicationScoped
@Path("/api/admin")
@Tag(name = "Admin", description = "Admin-only canary endpoints")
class AdminPingResource(
    private val currentUserProvider: CurrentUserProvider
) {
    @GET
    @Path("/ping")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    @Operation(summary = "Admin-only canary that proves role enforcement")
    @APIResponse(responseCode = "200", description = "Pong with the caller's subject id")
    @APIResponse(responseCode = "403", description = "Caller lacks the ADMIN role")
    fun ping(): PingDto = PingDto(
        message = "pong",
        user = currentUserProvider.get().subjectId
    )
}
