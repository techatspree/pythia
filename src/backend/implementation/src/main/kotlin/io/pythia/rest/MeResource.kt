package io.pythia.rest

import io.pythia.auth.CurrentUserProvider
import io.pythia.auth.Role
import io.pythia.i18n.fromCode
import io.pythia.service.CurrentUserService
import io.pythia.service.preferredLanguage
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.HttpHeaders
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
    val providerName: String,
    val language: String
)

data class LanguageUpdateDto(
    val language: String
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
    fun me(@HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) acceptLanguage: String?): Response {
        val u = currentUserProvider.current
            ?: return Response.status(Response.Status.UNAUTHORIZED).build()
        // ensureUser seeds language from Accept-Language on first sighting only;
        // read the persisted preference back off the returned User (not the
        // token principal, which carries no stored preference).
        val user = currentUserService.ensureUser(u, preferredLanguage(acceptLanguage))
        return Response.ok(
            CurrentUserDto(
                subjectId = u.subjectId,
                email = u.email,
                displayName = u.displayName,
                roles = u.roles.toList(),
                providerName = u.providerName,
                language = user.language
            )
        ).build()
    }

    @PUT
    @Path("/me/language")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update the current user's language preference")
    @APIResponse(responseCode = "204", description = "Language preference updated")
    @APIResponse(responseCode = "400", description = "Unsupported language code")
    @APIResponse(responseCode = "401", description = "No user is populated for the request")
    fun updateLanguage(body: LanguageUpdateDto): Response {
        val u = currentUserProvider.current
            ?: return Response.status(Response.Status.UNAUTHORIZED).build()
        val language = fromCode(body.language)
            ?: throw BadRequestException("Unsupported language: ${body.language} (use de or en)")
        currentUserService.updateLanguage(u, language)
        return Response.noContent().build()
    }
}
