package io.pythia.rest

import io.pythia.repository.UserRepository
import io.pythia.rest.dto.UserRefDto
import io.pythia.rest.dto.toRefDto
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.tags.Tag

// Read-only list of users, for the project-owner picker (task-200). Users are
// provisioned just in time, so only people who have used Pythia at least once
// appear here.
@Path("/api/users")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("VIEWER")
@Tag(name = "Users", description = "Provisioned users")
class UserResource(
    private val userRepository: UserRepository
) {

    @GET
    @Operation(summary = "List all provisioned users, sorted by display name")
    @APIResponse(
        responseCode = "200",
        description = "The users",
        content = [Content(schema = Schema(type = SchemaType.ARRAY, implementation = UserRefDto::class))]
    )
    fun listUsers(): Response =
        Response.ok(userRepository.listByDisplayName().map { it.toRefDto() }).build()
}
