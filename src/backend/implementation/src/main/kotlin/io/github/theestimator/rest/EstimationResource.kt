package io.github.theestimator.rest

import io.github.theestimator.repository.EstimationRepository
import io.github.theestimator.rest.dto.toEstimationDetailDto
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

@Path("/api/estimations")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
class EstimationResource(
    private val estimationRepository: EstimationRepository
) {

    @GET
    @Path("/{id}")
    fun getEstimation(@PathParam("id") id: UUID): Response {
        val estimation = estimationRepository.findById(id)
            ?: throw NotFoundException("Estimation not found: $id")
        return Response.ok(estimation.toEstimationDetailDto()).build()
    }
}
