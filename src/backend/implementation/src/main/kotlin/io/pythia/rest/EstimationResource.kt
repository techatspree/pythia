package io.pythia.rest

import io.pythia.repository.EstimationRepository
import io.pythia.rest.dto.EstimationDetailDto
import io.pythia.rest.dto.toEstimationDetailDto
import io.pythia.service.EstimationVersionService
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import java.util.UUID

@Path("/api/estimations")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("VIEWER")
@Tag(name = "Estimations", description = "Estimations and their draft calculation")
class EstimationResource(
    private val estimationRepository: EstimationRepository,
    private val versionService: EstimationVersionService
) {

    @GET
    @Path("/{id}")
    @Operation(summary = "Get an estimation with its live draft total effort")
    @APIResponse(
        responseCode = "200",
        description = "The estimation",
        content = [Content(schema = Schema(implementation = EstimationDetailDto::class))]
    )
    @APIResponse(responseCode = "404", description = "Estimation not found")
    fun getEstimation(@PathParam("id") id: UUID): Response {
        val estimation = estimationRepository.findById(id)
            ?: throw NotFoundException("Estimation not found: $id")
        val draftTotalEffort = estimation.draftVersion?.let {
            versionService.calculateDraft(it).totalEffort
        }
        return Response.ok(estimation.toEstimationDetailDto(draftTotalEffort)).build()
    }
}
