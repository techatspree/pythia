package io.github.theestimator.service

import io.github.theestimator.domain.draft.DraftEstimationNode
import io.github.theestimator.domain.draft.DraftEstimationVersion
import io.github.theestimator.domain.draft.DraftGroupNode
import io.github.theestimator.domain.draft.DraftTimeRelativeItemNode
import io.github.theestimator.rest.dto.AdditionalCostUpdateDto
import io.github.theestimator.rest.dto.DraftUpdateDto
import io.github.theestimator.rest.dto.EffortDriverDto
import io.github.theestimator.rest.dto.EstimationNodeUpdateDto
import io.github.theestimator.rest.dto.EstimationParameterDto
import io.github.theestimator.rest.dto.PhaseUpdateDto

// Captures a draft entity's full current state as the wire DraftUpdateDto —
// the inverse of DraftUpdateApplier. `apply(draft, draft.toUpdateDto())` is an
// identity on state; the Undo service uses this to snapshot before/after.
fun DraftEstimationVersion.toUpdateDto(): DraftUpdateDto = DraftUpdateDto(
    notes = notes,
    parameters = parameters.map {
        EstimationParameterDto(name = it.name, value = it.value, comment = it.comment)
    },
    effortDrivers = effortDrivers.map {
        EffortDriverDto(description = it.description, factor = it.factor, comment = it.comment)
    },
    phases = phases.map {
        PhaseUpdateDto(name = it.name, abbreviation = it.abbreviation, durationWeeks = it.durationWeeks)
    },
    roots = roots.map { it.toUpdateDto() },
    additionalCosts = additionalCosts.map {
        AdditionalCostUpdateDto(
            description = it.description,
            amount = it.amount,
            type = it.type,
            amountPerWeek = it.amountPerWeek,
            phaseAbbreviation = it.phase?.abbreviation
        )
    }
)

private fun DraftEstimationNode.toUpdateDto(): EstimationNodeUpdateDto {
    val nodeType = when (this) {
        is DraftGroupNode -> "GROUP"
        is DraftTimeRelativeItemNode -> "TIME_RELATIVE"
        else -> "FIXED"
    }
    return EstimationNodeUpdateDto(
        logicalId = logicalId,
        type = nodeType,
        title = title,
        description = description,
        code = code,
        minEffort = minEffort,
        expectedEffort = expectedEffort,
        maxEffort = maxEffort,
        assumptions = assumptions,
        unit = unit,
        phaseAbbreviation = phase?.abbreviation,
        children = children.map { it.toUpdateDto() }
    )
}
