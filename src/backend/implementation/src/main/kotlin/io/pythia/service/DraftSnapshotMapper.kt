package io.pythia.service

import io.pythia.domain.draft.DraftBucketedItemNode
import io.pythia.domain.draft.DraftEstimationNode
import io.pythia.domain.draft.DraftEstimationVersion
import io.pythia.domain.draft.DraftGroupNode
import io.pythia.domain.draft.DraftTimeRelativeItemNode
import io.pythia.rest.dto.AdditionalCostUpdateDto
import io.pythia.rest.dto.BucketUpdateDto
import io.pythia.rest.dto.DraftUpdateDto
import io.pythia.rest.dto.EffortDriverDto
import io.pythia.rest.dto.EstimationNodeUpdateDto
import io.pythia.rest.dto.PhaseUpdateDto

// Captures a draft entity's full current state as the wire DraftUpdateDto —
// the inverse of DraftUpdateApplier. `apply(draft, draft.toUpdateDto())` is an
// identity on state; the Undo service uses this to snapshot before/after.
fun DraftEstimationVersion.toUpdateDto(): DraftUpdateDto = DraftUpdateDto(
    // Non-null so a snapshot is a FULL state: DraftUpdateApplier treats a null
    // field as "leave unchanged" (wire-PUT semantics), which would make undo
    // unable to clear notes. Every list field below is likewise always present.
    notes = notes ?: "",
    dailyRate = dailyRate,
    stdDevFactor = stdDevFactor,
    salesSurcharge = salesSurcharge,
    effortDrivers = effortDrivers.map {
        EffortDriverDto(description = it.description, factor = it.factor, comment = it.comment)
    },
    phases = phases.map {
        PhaseUpdateDto(name = it.name, abbreviation = it.abbreviation, durationWeeks = it.durationWeeks)
    },
    // Buckets live on the estimation (shared across versions); capture them so
    // an undo/redo restores the bucket set (empty for PERT estimations).
    buckets = estimation?.buckets?.map {
        BucketUpdateDto(id = it.id, position = it.position, label = it.label)
    } ?: emptyList(),
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
        is DraftBucketedItemNode -> "BUCKETED"
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
        children = children.map { it.toUpdateDto() },
        bucketId = (this as? DraftBucketedItemNode)?.bucket?.id?.toString(),
        isSample = (this as? DraftBucketedItemNode)?.isSample ?: false
    )
}
