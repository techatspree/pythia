package io.github.theestimator.rest

import io.github.theestimator.domain.draft.DraftAdditionalCost
import io.github.theestimator.domain.draft.DraftEffortDriver
import io.github.theestimator.domain.draft.DraftEstimationNode
import io.github.theestimator.domain.draft.DraftEstimationParameter
import io.github.theestimator.domain.draft.DraftEstimationVersion
import io.github.theestimator.domain.draft.DraftFixedItemNode
import io.github.theestimator.domain.draft.DraftGroupNode
import io.github.theestimator.domain.draft.DraftProjectPhase
import io.github.theestimator.domain.draft.DraftTimeRelativeItemNode
import io.github.theestimator.domain.submitted.SubmittedEstimationVersion
import io.github.theestimator.repository.EstimationRepository
import io.github.theestimator.rest.dto.AdditionalCostUpdateDto
import io.github.theestimator.rest.dto.DraftUpdateDto
import io.github.theestimator.rest.dto.EffortDriverDto
import io.github.theestimator.rest.dto.EstimationNodeUpdateDto
import io.github.theestimator.rest.dto.EstimationParameterDto
import io.github.theestimator.rest.dto.EstimationVersionSummaryDto
import io.github.theestimator.rest.dto.PhaseUpdateDto
import io.github.theestimator.rest.dto.toDto
import io.github.theestimator.rest.dto.toSummaryDto
import io.github.theestimator.service.CsvExporter
import io.github.theestimator.service.EstimationVersionService
import io.github.theestimator.service.ExcelExporter
import io.github.theestimator.service.VersionComparisonService
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

// updateDraft's per-collection apply logic is split into focused private
// helpers to keep each method simple; that deliberately raises this resource's
// function count past the TooManyFunctions threshold for one cohesive endpoint.
@Suppress("TooManyFunctions")
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

        update.parameters?.let { applyParameters(draft, it) }
        update.effortDrivers?.let { applyEffortDrivers(draft, it) }
        update.phases?.let { applyPhases(draft, it) }
        update.roots?.let { applyRoots(draft, it) }
        update.additionalCosts?.let { applyAdditionalCosts(draft, it) }

        val result = versionService.calculateDraft(draft)
        return Response.ok(draft.toDto(result)).build()
    }

    private fun applyParameters(draft: DraftEstimationVersion, params: List<EstimationParameterDto>) {
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

    private fun applyEffortDrivers(draft: DraftEstimationVersion, drivers: List<EffortDriverDto>) {
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

    private fun applyPhases(draft: DraftEstimationVersion, phaseDtos: List<PhaseUpdateDto>) {
        // Upsert by abbreviation rather than clear-and-rebuild: persistent
        // DraftEstimationNode rows hold Java references to phase entities
        // by object identity, so orphan-removing them would leave dangling
        // references that fail Hibernate's pre-flush transient-reference
        // check (see EstimationVersionResourceIT "PUT replacing only phases
        // while persistent nodes reference them …").
        val keptAbbreviations = phaseDtos.map { it.abbreviation }.toSet()
        draft.phases.removeAll { it.abbreviation !in keptAbbreviations }
        val byAbbr = draft.phases.associateBy { it.abbreviation }
        phaseDtos.forEach { dto ->
            val existing = byAbbr[dto.abbreviation]
            if (existing != null) {
                existing.name = dto.name
                existing.durationWeeks = dto.durationWeeks
            } else {
                draft.phases.add(DraftProjectPhase().apply {
                    name = dto.name
                    abbreviation = dto.abbreviation
                    durationWeeks = dto.durationWeeks
                    version = draft
                })
            }
        }
    }

    private fun applyRoots(draft: DraftEstimationVersion, rootDtos: List<EstimationNodeUpdateDto>) {
        draft.roots.clear()
        rootDtos.forEachIndexed { idx, dto ->
            draft.roots.add(buildDraftNode(draft, dto, null, idx))
        }
    }

    private fun buildDraftNode(
        draft: DraftEstimationVersion,
        dto: EstimationNodeUpdateDto,
        parentNode: DraftEstimationNode?,
        pos: Int
    ): DraftEstimationNode {
        val node: DraftEstimationNode = when (dto.type) {
            "GROUP" -> DraftGroupNode().apply { title = dto.title }
            "TIME_RELATIVE" -> DraftTimeRelativeItemNode().apply { unit = dto.unit ?: "h/Woche" }
            else -> DraftFixedItemNode()
        }
        node.apply {
            logicalId = dto.logicalId ?: UUID.randomUUID()
            position = pos
            version = draft
            parent = parentNode
            if (dto.type != "GROUP") {
                description = dto.description
                code = dto.code
                minEffort = dto.minEffort
                expectedEffort = dto.expectedEffort
                maxEffort = dto.maxEffort
                assumptions = dto.assumptions
                phase = dto.phaseAbbreviation?.let { abbr ->
                    draft.phases.find { it.abbreviation == abbr }
                }
            }
        }
        dto.children.forEachIndexed { idx, childDto ->
            node.children.add(buildDraftNode(draft, childDto, node, idx))
        }
        return node
    }

    private fun applyAdditionalCosts(draft: DraftEstimationVersion, costDtos: List<AdditionalCostUpdateDto>) {
        draft.additionalCosts.clear()
        costDtos.forEach { dto ->
            val costPhase = dto.phaseAbbreviation?.let { abbr ->
                draft.phases.find { it.abbreviation == abbr }
            }
            draft.additionalCosts.add(DraftAdditionalCost().apply {
                description = dto.description
                amount = dto.amount
                type = dto.type
                amountPerWeek = dto.amountPerWeek
                phase = costPhase
                version = draft
            })
        }
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
