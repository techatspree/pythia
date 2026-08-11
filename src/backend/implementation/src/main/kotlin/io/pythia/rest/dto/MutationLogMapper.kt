package io.pythia.rest.dto

import io.pythia.domain.draft.DraftMutationLogEntry
import io.pythia.method.bucketsampled.BucketedEstimationItem
import io.pythia.method.threepoint.FixedEstimationItem
import io.pythia.method.threepoint.TimeRelativeEstimationItem
import io.pythia.model.AdditionalCost
import io.pythia.model.EffortDriver
import io.pythia.model.EstimationDefaults
import io.pythia.model.EstimationGroup
import io.pythia.model.EstimationNode
import io.pythia.model.EstimationVersion
import io.pythia.model.ProjectPhase
import io.pythia.model.mutation.AdditionalCostAdded
import io.pythia.model.mutation.AdditionalCostChanged
import io.pythia.model.mutation.AdditionalCostRemoved
import io.pythia.model.mutation.ChangeDescription
import io.pythia.model.mutation.EffortDriverAdded
import io.pythia.model.mutation.EffortDriverChanged
import io.pythia.model.mutation.EffortDriverRemoved
import io.pythia.model.mutation.NodeAdded
import io.pythia.model.mutation.NodeMoved
import io.pythia.model.mutation.NodeRemoved
import io.pythia.model.mutation.NodeRenamed
import io.pythia.model.mutation.NodeValueChanged
import io.pythia.model.mutation.NotesChanged
import io.pythia.model.mutation.OtherChange
import io.pythia.model.mutation.ParameterChanged
import io.pythia.model.mutation.PhaseAdded
import io.pythia.model.mutation.PhaseChanged
import io.pythia.model.mutation.PhaseRemoved
import io.pythia.model.mutation.Truncated
import io.pythia.model.mutation.diffSummary
import io.pythia.service.DraftMutationJackson
import io.quarkus.logging.Log
import java.util.UUID

// Entry → DTO (task-110: now enriched with a per-mutation change summary). Reads
// the LAZY `user` association, so call within an active transaction (the history
// endpoint is @Transactional). The summary is computed on the fly from the stored
// before/after DraftUpdateDto snapshots via the domain EstimationVersion.diffSummary
// — no schema change, no duplicate storage.
fun DraftMutationLogEntry.toLogDto(jackson: DraftMutationJackson) = MutationLogEntryDto(
    id = id!!,
    sequenceNumber = sequenceNumber,
    revisionBefore = revisionBefore,
    revisionAfter = revisionAfter,
    userId = user!!.id!!,
    userDisplayName = user!!.displayName ?: "",
    kind = kind ?: "",
    status = status.name,
    createdAt = createdAt!!,
    undoneAt = undoneAt,
    summary = summarise(jackson)
)

// Never fails the history endpoint: a payload that cannot be deserialised into a
// valid before/after pair (e.g. schema drift on a very old row) falls back to a
// single OtherChange entry and a WARN.
@Suppress("TooGenericExceptionCaught")
private fun DraftMutationLogEntry.summarise(jackson: DraftMutationJackson): List<ChangeSummaryDto> = try {
    val stored = jackson.fromJson(payload ?: error("mutation $id has no payload"))
    val changes = stored.before.toDomainVersion().diffSummary(stored.after.toDomainVersion())
    Log.debug("Summarised mutation $id: ${changes.size} change(s)")
    changes.map { it.toSummaryDto() }
} catch (e: Exception) {
    Log.warn("Could not summarise mutation $id, falling back to kind: ${e.message}")
    listOf(ChangeSummaryDto(kind = "OTHER_CHANGE", field = kind))
}

@Suppress("CyclomaticComplexMethod") // exhaustive one-liner map over the sealed set
private fun ChangeDescription.toSummaryDto(): ChangeSummaryDto = when (this) {
    is ParameterChanged -> ChangeSummaryDto(kind, field = name, oldValue = oldValue, newValue = newValue)
    is PhaseAdded -> ChangeSummaryDto(kind, path = title)
    is PhaseRemoved -> ChangeSummaryDto(kind, path = title)
    is PhaseChanged -> ChangeSummaryDto(kind, path = title, field = field, oldValue = oldValue, newValue = newValue)
    is EffortDriverAdded -> ChangeSummaryDto(kind, path = name)
    is EffortDriverRemoved -> ChangeSummaryDto(kind, path = name)
    is EffortDriverChanged -> ChangeSummaryDto(kind, path = name, field = field, oldValue = oldValue, newValue = newValue)
    is AdditionalCostAdded -> ChangeSummaryDto(kind, path = title)
    is AdditionalCostRemoved -> ChangeSummaryDto(kind, path = title)
    is AdditionalCostChanged ->
        ChangeSummaryDto(kind, path = title, field = field, oldValue = oldValue, newValue = newValue)
    is NodeAdded -> ChangeSummaryDto(kind, path = path, nodeType = nodeType)
    is NodeRemoved -> ChangeSummaryDto(kind, path = path, nodeType = nodeType)
    is NodeRenamed -> ChangeSummaryDto(kind, path = pathBefore, oldValue = oldTitle, newValue = newTitle)
    is NodeMoved -> ChangeSummaryDto(kind, path = pathBefore, newValue = pathAfter)
    is NodeValueChanged -> ChangeSummaryDto(kind, path = path, field = field, oldValue = oldValue, newValue = newValue)
    is NotesChanged -> ChangeSummaryDto(kind)
    is OtherChange -> ChangeSummaryDto(kind, field = otherKind)
    is Truncated -> ChangeSummaryDto(kind, remaining = remaining)
}

// Reconstructs a domain EstimationVersion from a stored DraftUpdateDto snapshot,
// carrying only the INPUT fields the diff compares (calculated values are
// irrelevant to the summary). Mirrors the DraftVersionMapper conversion without
// needing a persistence context.
private fun DraftUpdateDto.toDomainVersion(): EstimationVersion {
    val phaseByAbbr = (phases ?: emptyList()).associate {
        it.abbreviation to ProjectPhase(it.name, it.abbreviation, it.durationWeeks ?: 0.0)
    }
    return EstimationVersion(
        versionNumber = 0,
        notes = notes ?: "",
        dailyRate = dailyRate ?: EstimationDefaults.DAILY_RATE,
        stdDevFactor = stdDevFactor ?: EstimationDefaults.STD_DEV_FACTOR,
        salesSurcharge = salesSurcharge ?: EstimationDefaults.SALES_SURCHARGE,
        effortDrivers = (effortDrivers ?: emptyList()).map {
            EffortDriver(description = it.description, factor = it.factor, comment = it.comment ?: "")
        },
        phases = phaseByAbbr.values.toList(),
        additionalCosts = (additionalCosts ?: emptyList()).map {
            AdditionalCost(
                description = it.description,
                amount = it.amount,
                type = io.pythia.model.AdditionalCostType.valueOf(it.type.name),
                amountPerWeek = it.amountPerWeek ?: 0.0,
                phase = it.phaseAbbreviation?.let { abbr -> phaseByAbbr[abbr] }
            )
        },
        roots = (roots ?: emptyList()).map { it.toDomainNode(phaseByAbbr) }
    )
}

private fun EstimationNodeUpdateDto.toDomainNode(phaseByAbbr: Map<String, ProjectPhase>): EstimationNode {
    val lid = logicalId?.toString() ?: UUID.randomUUID().toString()
    val nodePhase = phaseAbbreviation?.let { phaseByAbbr[it] }
    return when (type) {
        "GROUP" -> EstimationGroup(
            title = title ?: "",
            children = children.map { it.toDomainNode(phaseByAbbr) },
            _logicalId = lid
        )
        "TIME_RELATIVE" -> TimeRelativeEstimationItem(
            unit = unit ?: "h/Woche",
            _description = description ?: "",
            _minEffort = minEffort ?: 0.0,
            _expectedEffort = expectedEffort ?: 0.0,
            _maxEffort = maxEffort ?: 0.0,
            _assumptions = assumptions ?: "",
            _phase = nodePhase,
            _logicalId = lid
        )
        "BUCKETED" -> BucketedEstimationItem(
            bucketId = bucketId ?: "",
            isSample = isSample,
            optimistic = minEffort,
            likely = expectedEffort,
            pessimistic = maxEffort,
            _description = description ?: "",
            _logicalId = lid,
            _phase = nodePhase
        )
        else -> FixedEstimationItem(
            _description = description ?: "",
            _minEffort = minEffort ?: 0.0,
            _expectedEffort = expectedEffort ?: 0.0,
            _maxEffort = maxEffort ?: 0.0,
            _assumptions = assumptions ?: "",
            _phase = nodePhase,
            _logicalId = lid
        )
    }
}
