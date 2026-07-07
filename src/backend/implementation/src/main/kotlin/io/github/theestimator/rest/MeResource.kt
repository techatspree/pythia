package io.github.theestimator.rest

import io.github.theestimator.auth.CurrentUserProvider
import io.github.theestimator.auth.Role
import io.github.theestimator.service.CurrentUserService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.tags.Tag

data class CurrentUserDto(
    val subjectId: String,
    val email: String?,
    val displayName: String?,
    val roles: List<Role>,
    val providerName: String
)

@ApplicationScoped
@Path("/api/auth")
@Tag(name = "Auth", description = "Current authenticated user")
class MeResource(
    private val currentUserProvider: CurrentUserProvider,
    private val currentUserService: CurrentUserService
) {

    @GET
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "The current authenticated user")
    @APIResponse(
        responseCode = "200",
        description = "The current user",
        content = [Content(schema = Schema(implementation = CurrentUserDto::class))]
    )
    @APIResponse(responseCode = "401", description = "No user is populated for the request")
    fun me(): Response {
        val u = currentUserProvider.current
            ?: return Response.status(Response.Status.UNAUTHORIZED).build()
        currentUserService.ensureUser(u)
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
