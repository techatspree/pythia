package io.github.theestimator.rest

import io.github.theestimator.repository.EstimationRepository
import io.github.theestimator.rest.dto.toEstimationDetailDto
import io.github.theestimator.service.EstimationVersionService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

@Path("/api/estimations")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
class EstimationResource(
    private val estimationRepository: EstimationRepository,
    private val versionService: EstimationVersionService
) {

    @GET
    @Path("/{id}")
    fun getEstimation(@PathParam("id") id: UUID): Response {
        val estimation = estimationRepository.findById(id)
            ?: throw NotFoundException("Estimation not found: $id")
        val draftTotalEffort = estimation.draftVersion?.let {
            versionService.calculateDraft(it).totalEffort
        }
        return Response.ok(estimation.toEstimationDetailDto(draftTotalEffort)).build()
    }
}
