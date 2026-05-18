package io.github.theestimator.rest

import io.github.theestimator.domain.draft.DraftEffortDriver
import io.github.theestimator.domain.draft.DraftEstimationItem
import io.github.theestimator.domain.draft.DraftEstimationItemGroup
import io.github.theestimator.domain.draft.DraftEstimationParameter
import io.github.theestimator.domain.draft.DraftFixedEstimationItem
import io.github.theestimator.domain.draft.DraftProjectPhase
import io.github.theestimator.domain.draft.DraftTimeRelativeEstimationItem
import io.github.theestimator.domain.submitted.SubmittedEstimationVersion
import io.github.theestimator.repository.EstimationRepository
import io.github.theestimator.rest.dto.*
import io.github.theestimator.service.CsvExporter
import io.github.theestimator.service.EstimationVersionService
import io.github.theestimator.service.ExcelExporter
import io.github.theestimator.service.VersionComparisonService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.StreamingOutput
import java.util.UUID

@Path("/api/estimations/{estimationId}/versions")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
class EstimationVersionResource(
    private val versionService: EstimationVersionService,
    private val comparisonService: VersionComparisonService,
    private val estimationRepository: EstimationRepository,
    private val excelExporter: ExcelExporter,
    private val csvExporter: CsvExporter
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

        update.phases?.let { phaseDtos ->
            draft.phases.clear()
            phaseDtos.forEach { dto ->
                draft.phases.add(DraftProjectPhase().apply {
                    name = dto.name
                    abbreviation = dto.abbreviation
                    durationWeeks = dto.durationWeeks
                    version = draft
                })
            }
        }

        update.itemGroups?.let { groupDtos ->
            draft.itemGroups.clear()
            groupDtos.forEach { groupDto ->
                val group = DraftEstimationItemGroup().apply {
                    logicalId = groupDto.logicalId ?: UUID.randomUUID()
                    title = groupDto.title
                    version = draft
                }
                groupDto.items.forEach { itemDto ->
                    val itemPhase = itemDto.phaseAbbreviation?.let { abbr ->
                        draft.phases.find { it.abbreviation == abbr }
                    }
                    val draftItem: DraftEstimationItem = if (itemDto.type == "TIME_RELATIVE")
                        DraftTimeRelativeEstimationItem().also { it.unit = itemDto.unit ?: "h/Woche" }
                    else
                        DraftFixedEstimationItem()
                    group.items.add(draftItem.apply {
                        logicalId = itemDto.logicalId ?: UUID.randomUUID()
                        description = itemDto.description
                        minEffort = itemDto.minEffort
                        expectedEffort = itemDto.expectedEffort
                        maxEffort = itemDto.maxEffort
                        assumptions = itemDto.assumptions
                        this.phase = itemPhase
                        this.group = group
                    })
                }
                draft.itemGroups.add(group)
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

    @GET
    @Path("/{versionA}/compare/{versionB}")
    fun compareVersions(
        @PathParam("estimationId") estimationId: UUID,
        @PathParam("versionA") versionA: String,
        @PathParam("versionB") versionB: String
    ): Response {
        ensureEstimationExists(estimationId)
        fun resolve(ref: String): SubmittedEstimationVersion {
            if (ref == "draft")
                return versionService.findDraft(estimationId)
                    ?.let { versionService.snapshotDraft(it) }
                    ?: throw NotFoundException("No draft found")
            val n = ref.toIntOrNull()
                ?: throw NotFoundException("Version $ref not found")
            return versionService.findSubmittedVersion(estimationId, n)
                ?: throw NotFoundException("Version $ref not found")
        }
        val a = resolve(versionA)
        val b = resolve(versionB)
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
        val version =
            if (versionNumber == "draft")
                versionService.findDraft(estimationId)
                    ?.let { versionService.snapshotDraft(it) }
                    ?: throw NotFoundException("No draft found")
            else
                versionService.findSubmittedVersion(
                    estimationId,
                    versionNumber.toIntOrNull()
                        ?: throw NotFoundException("Version $versionNumber not found")
                ) ?: throw NotFoundException("Version $versionNumber not found")

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

    private fun ensureEstimationExists(estimationId: UUID) {
        estimationRepository.findById(estimationId)
            ?: throw NotFoundException("Estimation not found: $estimationId")
    }
}
