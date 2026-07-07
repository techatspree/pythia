package io.github.theestimator.rest

import io.github.theestimator.auth.CurrentUserProvider
import io.github.theestimator.domain.submitted.SubmittedEstimationVersion
import io.github.theestimator.repository.EstimationRepository
import io.github.theestimator.rest.dto.ConflictDetailsDto
import io.github.theestimator.rest.dto.DraftUpdateDto
import io.github.theestimator.rest.dto.EstimationVersionDto
import io.github.theestimator.rest.dto.EstimationVersionSummaryDto
import io.github.theestimator.rest.dto.MutationLogEntryDto
import io.github.theestimator.rest.dto.VersionComparisonDto
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
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import java.util.UUID

@Path("/api/estimations/{estimationId}/versions")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("VIEWER")
@Tag(name = "Estimation versions", description = "Draft editing, undo/redo, submitted snapshots, compare and export")
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
    @Operation(summary = "List versions (live draft first, then submitted snapshots)")
    @APIResponse(
        responseCode = "200",
        description = "The version summaries",
        content = [
            Content(schema = Schema(type = SchemaType.ARRAY, implementation = EstimationVersionSummaryDto::class))
        ]
    )
    @APIResponse(responseCode = "404", description = "Estimation not found")
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
    @Operation(summary = "Create a draft version (cloned from the latest submitted, if any)")
    @APIResponse(
        responseCode = "201",
        description = "The created draft with calculated values",
        content = [Content(schema = Schema(implementation = EstimationVersionDto::class))]
    )
    @APIResponse(responseCode = "404", description = "Estimation not found")
    @APIResponse(responseCode = "409", description = "A draft already exists for this estimation")
    fun createDraft(@PathParam("estimationId") estimationId: UUID): Response {
        ensureEstimationExists(estimationId)
        val draft = versionService.createDraft(estimationId)
        val result = versionService.calculateDraft(draft)
        return Response.status(Response.Status.CREATED).entity(draft.toDto(result)).build()
    }

    @GET
    @Path("/draft")
    @Operation(summary = "Get the draft with on-the-fly calculated values")
    @APIResponse(
        responseCode = "200",
        description = "The draft with calculated values",
        content = [Content(schema = Schema(implementation = EstimationVersionDto::class))]
    )
    @APIResponse(responseCode = "404", description = "No draft found for this estimation")
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
    @Operation(summary = "Replace the draft's collections wholesale and record the mutation")
    @APIResponse(
        responseCode = "200",
        description = "The updated draft with recalculated values",
        content = [Content(schema = Schema(implementation = EstimationVersionDto::class))]
    )
    @APIResponse(responseCode = "404", description = "No draft found for this estimation")
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
    @Operation(summary = "Snapshot the draft into an immutable submitted version")
    @APIResponse(
        responseCode = "200",
        description = "The submitted version snapshot",
        content = [Content(schema = Schema(implementation = EstimationVersionDto::class))]
    )
    @APIResponse(responseCode = "404", description = "No draft found for this estimation")
    fun submitDraft(@PathParam("estimationId") estimationId: UUID): Response {
        ensureEstimationExists(estimationId)
        val submitted = versionService.submitDraft(estimationId)
        return Response.ok(submitted.toDto()).build()
    }

    @POST
    @Path("/draft/undo")
    @Transactional
    @RolesAllowed("ESTIMATOR")
    @Operation(summary = "Undo this user's last draft mutation")
    @APIResponse(
        responseCode = "200",
        description = "The recalculated draft",
        content = [Content(schema = Schema(implementation = EstimationVersionDto::class))]
    )
    @APIResponse(responseCode = "404", description = "No draft found for this estimation")
    @APIResponse(
        responseCode = "409",
        description = "A newer change blocks the undo",
        content = [Content(schema = Schema(implementation = ConflictDetailsDto::class))]
    )
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
    @Operation(summary = "Redo this user's last undone draft mutation")
    @APIResponse(
        responseCode = "200",
        description = "The recalculated draft",
        content = [Content(schema = Schema(implementation = EstimationVersionDto::class))]
    )
    @APIResponse(responseCode = "404", description = "No draft found for this estimation")
    @APIResponse(
        responseCode = "409",
        description = "A newer change blocks the redo",
        content = [Content(schema = Schema(implementation = ConflictDetailsDto::class))]
    )
    fun redoDraft(@PathParam("estimationId") estimationId: UUID): Response {
        ensureEstimationExists(estimationId)
        val user = currentUserService.ensureUser(currentUserProvider.get())
        undoService.redoLastForUser(estimationId, user)
        return recalculatedDraftResponse(estimationId)
    }

    @GET
    @Path("/draft/history")
    @Transactional
    @Operation(summary = "The draft's mutation log (active and undone, ordered by sequence)")
    @APIResponse(
        responseCode = "200",
        description = "The mutation log entries",
        content = [Content(schema = Schema(type = SchemaType.ARRAY, implementation = MutationLogEntryDto::class))]
    )
    @APIResponse(responseCode = "404", description = "Estimation not found")
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
    @Operation(summary = "Delete the draft version")
    @APIResponse(responseCode = "204", description = "The draft was deleted")
    @APIResponse(responseCode = "404", description = "Estimation not found")
    fun deleteDraft(@PathParam("estimationId") estimationId: UUID): Response {
        ensureEstimationExists(estimationId)
        versionService.deleteDraft(estimationId)
        return Response.noContent().build()
    }

    @GET
    @Path("/{versionNumber}")
    @Operation(summary = "Read a submitted version (stored calculated values)")
    @APIResponse(
        responseCode = "200",
        description = "The submitted version",
        content = [Content(schema = Schema(implementation = EstimationVersionDto::class))]
    )
    @APIResponse(responseCode = "404", description = "Version not found for this estimation")
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
    @Operation(summary = "Diff two versions (use \"draft\" for the live draft)")
    @APIResponse(
        responseCode = "200",
        description = "The version comparison",
        content = [Content(schema = Schema(implementation = VersionComparisonDto::class))]
    )
    @APIResponse(responseCode = "404", description = "A referenced version was not found")
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
    @Operation(summary = "Export a version as xlsx (octet-stream) or csv")
    @APIResponse(
        responseCode = "200",
        description = "The exported file",
        content = [
            Content(mediaType = "application/octet-stream"),
            Content(mediaType = "text/csv")
        ]
    )
    @APIResponse(responseCode = "400", description = "Unsupported export format")
    @APIResponse(responseCode = "404", description = "Version not found for this estimation")
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
