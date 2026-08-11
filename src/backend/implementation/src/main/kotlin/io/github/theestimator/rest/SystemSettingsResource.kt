package io.github.theestimator.rest

import io.github.theestimator.auth.CurrentUserProvider
import io.github.theestimator.rest.dto.EffortDriverDto
import io.github.theestimator.rest.dto.SystemSettingsDto
import io.github.theestimator.rest.dto.SystemSettingsUpdateDto
import io.github.theestimator.service.SystemSettingsService
import io.quarkus.logging.Log
import jakarta.annotation.security.PermitAll
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
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
import org.jboss.resteasy.reactive.RestForm
import org.jboss.resteasy.reactive.multipart.FileUpload

// Per-installation system settings (task-146): the operating organisation's
// display name, its standard effort drivers, and an optional stylesheet that
// overrides the built-in brand styling.
//
// The two READS are @PermitAll on purpose: the auth gate renders the brand
// lockup with the login dialog BEFORE any account exists, so role-gating them
// would leave the login screen unbranded. They expose only public branding.
// Every WRITE is ADMIN — an uploaded stylesheet can overlay or spoof UI, so
// these must never be widened to ESTIMATOR.
@ApplicationScoped
@Path("/api/system")
@Tag(name = "System", description = "Per-installation settings: name, standard effort drivers, custom CSS")
class SystemSettingsResource(
    private val systemSettingsService: SystemSettingsService,
    private val currentUserProvider: CurrentUserProvider
) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    @Operation(summary = "Read the installation's settings (unauthenticated: the login screen needs them)")
    @APIResponse(
        responseCode = "200",
        content = [Content(schema = Schema(implementation = SystemSettingsDto::class))]
    )
    fun get(): Response = Response.ok(toDto()).build()

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    @Operation(summary = "Admin: set the installation's display name")
    @APIResponse(responseCode = "204", description = "Stored")
    @APIResponse(responseCode = "403", description = "Not an administrator")
    fun update(body: SystemSettingsUpdateDto): Response {
        Log.info("System settings updated by ${currentUserProvider.get().subjectId}")
        systemSettingsService.updateDisplayName(body.displayName)
        return Response.noContent().build()
    }

    @GET
    @Path("/css")
    @Produces("text/css")
    @PermitAll
    @Operation(summary = "The installation's custom stylesheet, if any")
    @APIResponse(responseCode = "200", description = "The stylesheet")
    @APIResponse(responseCode = "404", description = "No custom stylesheet is configured")
    fun css(): Response {
        val css = systemSettingsService.settings().customCss
            ?: return Response.status(Response.Status.NOT_FOUND).build()
        // The browser pulls this in as an ordinary <link> stylesheet, so without
        // no-cache an admin's change would not show up until a hard reload.
        return Response.ok(css)
            .header("Cache-Control", "no-cache")
            .build()
    }

    @PUT
    @Path("/css")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed("ADMIN")
    @Operation(summary = "Admin: upload a stylesheet that overrides the standard GUI styling")
    @APIResponse(responseCode = "204", description = "Stored")
    @APIResponse(responseCode = "400", description = "The upload is empty or larger than 256 KB")
    @APIResponse(responseCode = "403", description = "Not an administrator")
    fun uploadCss(@RestForm("file") file: FileUpload): Response {
        Log.info("Custom CSS uploaded by ${currentUserProvider.get().subjectId}")
        systemSettingsService.storeCss(file.fileName(), file.uploadedFile().toFile().readText(Charsets.UTF_8))
        return Response.noContent().build()
    }

    @DELETE
    @Path("/css")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Admin: remove the custom stylesheet")
    @APIResponse(responseCode = "204", description = "Removed")
    @APIResponse(responseCode = "403", description = "Not an administrator")
    fun deleteCss(): Response {
        Log.info("Custom CSS removed by ${currentUserProvider.get().subjectId}")
        systemSettingsService.clearCss()
        return Response.noContent().build()
    }

    @GET
    @Path("/effort-drivers")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("VIEWER")
    @Operation(summary = "The installation's standard effort drivers")
    @APIResponse(
        responseCode = "200",
        content = [Content(schema = Schema(implementation = EffortDriverDto::class, type = SchemaType.ARRAY))]
    )
    fun effortDrivers(): Response = Response.ok(driverDtos()).build()

    @PUT
    @Path("/effort-drivers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    @Operation(summary = "Admin: replace the standard effort drivers wholesale")
    @APIResponse(
        responseCode = "200",
        content = [Content(schema = Schema(implementation = EffortDriverDto::class, type = SchemaType.ARRAY))]
    )
    @APIResponse(responseCode = "403", description = "Not an administrator")
    fun replaceEffortDrivers(body: List<EffortDriverDto>): Response {
        Log.info("Standard effort drivers replaced by ${currentUserProvider.get().subjectId} (n=${body.size})")
        systemSettingsService.replaceStandardDrivers(body)
        return Response.ok(driverDtos()).build()
    }

    private fun toDto(): SystemSettingsDto {
        val s = systemSettingsService.settings()
        return SystemSettingsDto(
            displayName = s.displayName,
            hasCustomCss = s.customCss != null,
            customCssFilename = s.customCssFilename,
            customCssUpdatedAt = s.customCssUpdatedAt?.toString()
        )
    }

    private fun driverDtos(): List<EffortDriverDto> =
        systemSettingsService.standardDrivers().map {
            EffortDriverDto(id = it.id, description = it.description, factor = it.factor, comment = it.comment)
        }
}
