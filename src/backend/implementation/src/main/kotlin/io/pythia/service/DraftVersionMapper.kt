package io.pythia.service

import io.pythia.domain.draft.DraftAdditionalCost
import io.pythia.domain.draft.DraftEffortDriver
import io.pythia.domain.draft.DraftEstimationNode
import io.pythia.domain.draft.DraftEstimationVersion
import io.pythia.domain.draft.DraftBucketedItemNode
import io.pythia.domain.draft.DraftFixedItemNode
import io.pythia.domain.draft.DraftGroupNode
import io.pythia.domain.draft.DraftProjectPhase
import io.pythia.domain.draft.DraftTimeRelativeItemNode
import io.pythia.method.bucketsampled.BucketedEstimationItem
import io.pythia.method.threepoint.FixedEstimationItem
import io.pythia.method.threepoint.TimeRelativeEstimationItem
import io.pythia.model.AdditionalCost
import io.pythia.model.EffortDriver
import io.pythia.model.EstimationGroup
import io.pythia.model.EstimationNode
import io.pythia.model.EstimationVersion
import io.pythia.model.ProjectPhase
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
        type = io.pythia.model.AdditionalCostType.valueOf(type.name),
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
