package io.github.theestimator.rest

import io.github.theestimator.auth.CurrentUserProvider
import io.github.theestimator.domain.submitted.SubmittedEstimationVersion
import io.github.theestimator.repository.EstimationRepository
import io.github.theestimator.rest.dto.DraftUpdateDto
import io.github.theestimator.rest.dto.EstimationVersionSummaryDto
import io.github.theestimator.rest.dto.toDto
import io.github.theestimator.rest.dto.toLogDto
import io.github.theestimator.rest.dto.toSummaryDto
import io.github.theestimator.service.CsvExporter
import io.github.theestimator.service.CurrentUserService
import io.github.theestimator.service.DraftUpdateApplier
import io.github.theestimator.service.DraftVersionMapper
import io.github.theestimator.service.EstimationVersionService
import io.github.theestimator.service.ExcelExporter
import io.github.theestimator.service.UndoService
import io.github.theestimator.service.VersionComparisonService
import io.github.theestimator.service.toUpdateDto
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.StreamingOutput
import java.util.UUID

@Path("/api/estimations/{estimationId}/versions")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("VIEWER")
class EstimationVersionResource(
    private val versionService: EstimationVersionService,
    private val comparisonService: VersionComparisonService,
    private val estimationRepository: EstimationRepository,
    private val excelExporter: ExcelExporter,
    private val csvExporter: CsvExporter,
    private val draftUpdateApplier: DraftUpdateApplier,
    private val undoService: UndoService,
    private val draftVersionMapper: DraftVersionMapper,
    private val currentUserService: CurrentUserService,
    private val currentUserProvider: CurrentUserProvider
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
    @RolesAllowed("ESTIMATOR")
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
    @RolesAllowed("ESTIMATOR")
    fun updateDraft(
        @PathParam("estimationId") estimationId: UUID,
        update: DraftUpdateDto
    ): Response {
        ensureEstimationExists(estimationId)
        val draft = versionService.findDraft(estimationId)
            ?: throw NotFoundException("No draft found for estimation $estimationId")

        // @RolesAllowed guarantees an authenticated request, so `current` is
        // never null; UserProvisioningFilter has already provisioned the row,
        // so this just fetches the User entity to attribute the mutation to.
        val user = currentUserService.ensureUser(currentUserProvider.get())
        val beforeDto = draft.toUpdateDto()
        val before = draftVersionMapper.toDomain(draft)

        draftUpdateApplier.apply(draft, update)

        val afterDto = draft.toUpdateDto()
        val after = draftVersionMapper.toDomain(draft)
        undoService.recordMutation(draft, before, after, beforeDto, afterDto, user)

        val result = versionService.calculateDraft(draft)
        return Response.ok(draft.toDto(result)).build()
    }

    @POST
    @Path("/draft/submit")
    @Transactional
    @RolesAllowed("ESTIMATOR")
    fun submitDraft(@PathParam("estimationId") estimationId: UUID): Response {
        ensureEstimationExists(estimationId)
        val submitted = versionService.submitDraft(estimationId)
        return Response.ok(submitted.toDto()).build()
    }

    @POST
    @Path("/draft/undo")
    @Transactional
    @RolesAllowed("ESTIMATOR")
    fun undoDraft(@PathParam("estimationId") estimationId: UUID): Response {
        ensureEstimationExists(estimationId)
        val user = currentUserService.ensureUser(currentUserProvider.get())
        undoService.undoLastForUser(estimationId, user)
        return recalculatedDraftResponse(estimationId)
    }

    @POST
    @Path("/draft/redo")
    @Transactional
    @RolesAllowed("ESTIMATOR")
    fun redoDraft(@PathParam("estimationId") estimationId: UUID): Response {
        ensureEstimationExists(estimationId)
        val user = currentUserService.ensureUser(currentUserProvider.get())
        undoService.redoLastForUser(estimationId, user)
        return recalculatedDraftResponse(estimationId)
    }

    @GET
    @Path("/draft/history")
    @Transactional
    fun draftHistory(@PathParam("estimationId") estimationId: UUID): Response {
        ensureEstimationExists(estimationId)
        return Response.ok(undoService.historyFor(estimationId).map { it.toLogDto() }).build()
    }

    // The undo/redo mutation happens on the draft entity; re-read it and return
    // the recalculated DTO exactly as updateDraft does.
    private fun recalculatedDraftResponse(estimationId: UUID): Response {
        val draft = versionService.findDraft(estimationId)
            ?: throw NotFoundException("No draft found for estimation $estimationId")
        return Response.ok(draft.toDto(versionService.calculateDraft(draft))).build()
    }

    @DELETE
    @Path("/draft")
    @Transactional
    @RolesAllowed("ESTIMATOR")
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

    @GET
    @Path("/{versionA}/compare/{versionB}")
    fun compareVersions(
        @PathParam("estimationId") estimationId: UUID,
        @PathParam("versionA") versionA: String,
        @PathParam("versionB") versionB: String
    ): Response {
        ensureEstimationExists(estimationId)
        val a = resolveVersion(estimationId, versionA)
        val b = resolveVersion(estimationId, versionB)
        return Response.ok(comparisonService.compare(a, b)).build()
    }

    @GET
    @Path("/{versionNumber}/export")
    @Produces("application/octet-stream", "text/csv")
    fun exportVersion(
        @PathParam("estimationId") estimationId: UUID,
        @PathParam("versionNumber") versionNumber: String,
        @QueryParam("format") @DefaultValue("xlsx") format: String
    ): Response {
        ensureEstimationExists(estimationId)
        val version = resolveVersion(estimationId, versionNumber)

        val label = if (versionNumber == "draft") "draft" else "v$versionNumber"
        return when (format) {
            "xlsx" -> Response.ok(StreamingOutput { os -> excelExporter.export(version, os) })
                .type("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=\"estimation-$label.xlsx\"")
                .build()
            "csv" -> Response.ok(StreamingOutput { os -> csvExporter.export(version, os) })
                .type("text/csv")
                .header("Content-Disposition", "attachment; filename=\"estimation-$label.csv\"")
                .build()
            else -> throw BadRequestException("Unsupported format: $format (use xlsx or csv)")
        }
    }

    // Resolve a version ref ("draft" or a number) to a submitted snapshot, or
    // 404. Shared by compare and export; kept to <=2 throws.
    private fun resolveVersion(estimationId: UUID, ref: String): SubmittedEstimationVersion {
        if (ref == "draft") {
            return versionService.findDraft(estimationId)
                ?.let { versionService.snapshotDraft(it) }
                ?: throw NotFoundException("No draft found")
        }
        val version = ref.toIntOrNull()?.let { versionService.findSubmittedVersion(estimationId, it) }
        return version ?: throw NotFoundException("Version $ref not found")
    }

    private fun ensureEstimationExists(estimationId: UUID) {
        estimationRepository.findById(estimationId)
            ?: throw NotFoundException("Estimation not found: $estimationId")
    }
}
