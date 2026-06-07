package io.github.theestimator.service

import io.github.theestimator.domain.draft.*
import io.github.theestimator.model.*
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class DraftVersionMapper {

    fun toDomain(draft: DraftEstimationVersion): EstimationVersion {
        val phaseMap = draft.phases.associate { it.abbreviation to it.toDomain() }
        return EstimationVersion(
            versionNumber = draft.versionNumber,
            notes = draft.notes ?: "",
            parameters = draft.parameters.map { it.toDomain() },
            effortDrivers = draft.effortDrivers.map { it.toDomain() },
            phases = phaseMap.values.toList(),
            additionalCosts = draft.additionalCosts.map { it.toDomain(phaseMap) },
            roots = draft.roots.map { it.toDomain(phaseMap) }
        )
    }

    private fun DraftEstimationParameter.toDomain() = EstimationParameter(
        name = name,
        value = value,
        comment = comment ?: ""
    )

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
        is DraftGroupNode -> EstimationGroup(
            title = title ?: "",
            children = children.map { it.toDomain(phaseMap) },
            _logicalId = logicalId.toString()
        )
        is DraftTimeRelativeItemNode -> TimeRelativeEstimationItem(
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
        is DraftFixedItemNode -> FixedEstimationItem(
            _description = description ?: "",
            _code = code ?: "",
            _minEffort = minEffort ?: 0.0,
            _expectedEffort = expectedEffort ?: 0.0,
            _maxEffort = maxEffort ?: 0.0,
            _assumptions = assumptions ?: "",
            _phase = phase?.abbreviation?.let { phaseMap[it] },
            _logicalId = logicalId.toString()
        )
        else -> error("Unknown node type: ${this::class.simpleName}")
    }
}
