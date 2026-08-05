package io.github.theestimator.service

import io.github.theestimator.domain.draft.DraftAdditionalCost
import io.github.theestimator.domain.draft.DraftEffortDriver
import io.github.theestimator.domain.draft.DraftEstimationNode
import io.github.theestimator.domain.draft.DraftEstimationVersion
import io.github.theestimator.domain.draft.DraftBucketedItemNode
import io.github.theestimator.domain.draft.DraftFixedItemNode
import io.github.theestimator.domain.draft.DraftGroupNode
import io.github.theestimator.domain.draft.DraftProjectPhase
import io.github.theestimator.domain.draft.DraftTimeRelativeItemNode
import io.github.theestimator.method.bucketsampled.BucketedEstimationItem
import io.github.theestimator.method.threepoint.FixedEstimationItem
import io.github.theestimator.method.threepoint.TimeRelativeEstimationItem
import io.github.theestimator.model.AdditionalCost
import io.github.theestimator.model.EffortDriver
import io.github.theestimator.model.EstimationGroup
import io.github.theestimator.model.EstimationNode
import io.github.theestimator.model.EstimationVersion
import io.github.theestimator.model.ProjectPhase
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class DraftVersionMapper {

    fun toDomain(draft: DraftEstimationVersion): EstimationVersion {
        val phaseMap = draft.phases.associate { it.abbreviation to it.toDomain() }
        return EstimationVersion(
            versionNumber = draft.versionNumber,
            notes = draft.notes ?: "",
            dailyRate = draft.dailyRate,
            stdDevFactor = draft.stdDevFactor,
            salesSurcharge = draft.salesSurcharge,
            effortDrivers = draft.effortDrivers.map { it.toDomain() },
            phases = phaseMap.values.toList(),
            additionalCosts = draft.additionalCosts.map { it.toDomain(phaseMap) },
            roots = draft.roots.map { it.toDomain(phaseMap) }
        )
    }


    private fun DraftEffortDriver.toDomain() = EffortDriver(
        description = description,
        factor = factor,
        comment = comment ?: ""
    )

    private fun DraftProjectPhase.toDomain() = ProjectPhase(
        name = name,
        abbreviation = abbreviation,
        durationWeeks = durationWeeks ?: 0.0
    )

    private fun DraftAdditionalCost.toDomain(phaseMap: Map<String, ProjectPhase>) = AdditionalCost(
        description = description,
        amount = amount,
        type = io.github.theestimator.model.AdditionalCostType.valueOf(type.name),
        amountPerWeek = amountPerWeek ?: 0.0,
        phase = phase?.abbreviation?.let { phaseMap[it] }
    )

    private fun DraftEstimationNode.toDomain(phaseMap: Map<String, ProjectPhase>): EstimationNode = when (this) {
        is DraftGroupNode -> toDomainGroup(phaseMap)
        is DraftTimeRelativeItemNode -> toDomainTimeRelative(phaseMap)
        is DraftBucketedItemNode -> toDomainBucketed(phaseMap)
        is DraftFixedItemNode -> toDomainFixed(phaseMap)
        else -> error("Unknown node type: ${this::class.simpleName}")
    }

    private fun DraftBucketedItemNode.toDomainBucketed(phaseMap: Map<String, ProjectPhase>) =
        BucketedEstimationItem(
            bucketId = bucket?.id?.toString() ?: "",
            isSample = isSample ?: false,
            optimistic = minEffort,
            likely = expectedEffort,
            pessimistic = maxEffort,
            _description = description ?: "",
            _phase = phase?.abbreviation?.let { phaseMap[it] },
            _logicalId = logicalId.toString()
        )

    private fun DraftGroupNode.toDomainGroup(phaseMap: Map<String, ProjectPhase>) = EstimationGroup(
        title = title ?: "",
        children = children.map { it.toDomain(phaseMap) },
        _logicalId = logicalId.toString()
    )

    private fun DraftTimeRelativeItemNode.toDomainTimeRelative(phaseMap: Map<String, ProjectPhase>) =
        TimeRelativeEstimationItem(
            unit = unit ?: "h/Woche",
            _description = description ?: "",
            _code = code ?: "",
            _minEffort = minEffort ?: 0.0,
            _expectedEffort = expectedEffort ?: 0.0,
            _maxEffort = maxEffort ?: 0.0,
            _assumptions = assumptions ?: "",
            _phase = phase?.abbreviation?.let { phaseMap[it] },
            _logicalId = logicalId.toString()
        )

    private fun DraftFixedItemNode.toDomainFixed(phaseMap: Map<String, ProjectPhase>) = FixedEstimationItem(
        _description = description ?: "",
        _code = code ?: "",
        _minEffort = minEffort ?: 0.0,
        _expectedEffort = expectedEffort ?: 0.0,
        _maxEffort = maxEffort ?: 0.0,
        _assumptions = assumptions ?: "",
        _phase = phase?.abbreviation?.let { phaseMap[it] },
        _logicalId = logicalId.toString()
    )
}
