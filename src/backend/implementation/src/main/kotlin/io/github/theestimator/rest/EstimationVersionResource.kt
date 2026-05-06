package io.github.theestimator.rest

import io.github.theestimator.domain.draft.DraftEffortDriver
import io.github.theestimator.domain.draft.DraftEstimationParameter
import io.github.theestimator.repository.EstimationRepository
import io.github.theestimator.rest.dto.*
import io.github.theestimator.service.EstimationVersionService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

@Path("/api/estimations/{estimationId}/versions")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
class EstimationVersionResource(
    private val versionService: EstimationVersionService,
    private val estimationRepository: EstimationRepository
) {

    @GET
    fun listVersions(@PathParam("estimationId") estimationId: UUID): Response {
        ensureEstimationExists(estimationId)
        val submitted = versionService.findSubmittedVersions(estimationId)
        val draft = versionService.findDraft(estimationId)

        val summaries = mutableListOf<EstimationVersionSummaryDto>()
        if (draft != null) {
            val result = versionService.calculateDraft(draft)
            summaries.add(draft.toSummaryDto(result.totalEffort))
        }
        summaries.addAll(submitted.map { it.toSummaryDto() })

        return Response.ok(summaries).build()
    }

    @POST
    @Transactional
    fun createDraft(@PathParam("estimationId") estimationId: UUID): Response {
        ensureEstimationExists(estimationId)
        val draft = versionService.createDraft(estimationId)
        val result = versionService.calculateDraft(draft)
        return Response.status(Response.Status.CREATED).entity(draft.toDto(result)).build()
    }

    @GET
    @Path("/draft")
    fun getDraft(@PathParam("estimationId") estimationId: UUID): Response {
        ensureEstimationExists(estimationId)
        val draft = versionService.findDraft(estimationId)
            ?: throw NotFoundException("No draft found for estimation $estimationId")
        val result = versionService.calculateDraft(draft)
        return Response.ok(draft.toDto(result)).build()
    }

    @PUT
    @Path("/draft")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    fun updateDraft(
        @PathParam("estimationId") estimationId: UUID,
        update: DraftUpdateDto
    ): Response {
        ensureEstimationExists(estimationId)
        val draft = versionService.findDraft(estimationId)
            ?: throw NotFoundException("No draft found for estimation $estimationId")

        update.notes?.let { draft.notes = it }

        update.parameters?.let { params ->
            draft.parameters.clear()
            params.forEach { dto ->
                draft.parameters.add(DraftEstimationParameter().apply {
                    name = dto.name
                    value = dto.value
                    comment = dto.comment
                    version = draft
                })
            }
        }

        update.effortDrivers?.let { drivers ->
            draft.effortDrivers.clear()
            drivers.forEach { dto ->
                draft.effortDrivers.add(DraftEffortDriver().apply {
                    description = dto.description
                    factor = dto.factor
                    comment = dto.comment
                    version = draft
                })
            }
        }

        val result = versionService.calculateDraft(draft)
        return Response.ok(draft.toDto(result)).build()
    }

    @POST
    @Path("/draft/submit")
    @Transactional
    fun submitDraft(@PathParam("estimationId") estimationId: UUID): Response {
        ensureEstimationExists(estimationId)
        val submitted = versionService.submitDraft(estimationId)
        return Response.ok(submitted.toDto()).build()
    }

    @DELETE
    @Path("/draft")
    @Transactional
    fun deleteDraft(@PathParam("estimationId") estimationId: UUID): Response {
        ensureEstimationExists(estimationId)
        versionService.deleteDraft(estimationId)
        return Response.noContent().build()
    }

    @GET
    @Path("/{versionNumber}")
    fun getSubmittedVersion(
        @PathParam("estimationId") estimationId: UUID,
        @PathParam("versionNumber") versionNumber: Int
    ): Response {
        ensureEstimationExists(estimationId)
        val version = versionService.findSubmittedVersion(estimationId, versionNumber)
            ?: throw NotFoundException("Version $versionNumber not found for estimation $estimationId")
        return Response.ok(version.toDto()).build()
    }

    private fun ensureEstimationExists(estimationId: UUID) {
        estimationRepository.findById(estimationId)
            ?: throw NotFoundException("Estimation not found: $estimationId")
    }
}
