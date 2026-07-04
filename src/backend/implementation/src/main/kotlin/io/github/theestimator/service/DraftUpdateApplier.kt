package io.github.theestimator.service

import io.github.theestimator.domain.draft.DraftAdditionalCost
import io.github.theestimator.domain.draft.DraftEffortDriver
import io.github.theestimator.domain.draft.DraftEstimationNode
import io.github.theestimator.domain.draft.DraftEstimationParameter
import io.github.theestimator.domain.draft.DraftEstimationVersion
import io.github.theestimator.domain.draft.DraftFixedItemNode
import io.github.theestimator.domain.draft.DraftGroupNode
import io.github.theestimator.domain.draft.DraftProjectPhase
import io.github.theestimator.domain.draft.DraftTimeRelativeItemNode
import io.github.theestimator.rest.dto.AdditionalCostUpdateDto
import io.github.theestimator.rest.dto.DraftUpdateDto
import io.github.theestimator.rest.dto.EffortDriverDto
import io.github.theestimator.rest.dto.EstimationNodeUpdateDto
import io.github.theestimator.rest.dto.EstimationParameterDto
import io.github.theestimator.rest.dto.PhaseUpdateDto
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

// Shared clear-and-rebuild that writes a DraftUpdateDto into a draft entity.
// The canonical draft-write path used by both the REST PUT (updateDraft) and
// the Undo service (restoring a captured snapshot).
@ApplicationScoped
class DraftUpdateApplier {

    fun apply(draft: DraftEstimationVersion, update: DraftUpdateDto) {
        update.notes?.let { draft.notes = it }
        update.parameters?.let { applyParameters(draft, it) }
        update.effortDrivers?.let { applyEffortDrivers(draft, it) }
        update.phases?.let { applyPhases(draft, it) }
        update.roots?.let { applyRoots(draft, it) }
        update.additionalCosts?.let { applyAdditionalCosts(draft, it) }
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
}
