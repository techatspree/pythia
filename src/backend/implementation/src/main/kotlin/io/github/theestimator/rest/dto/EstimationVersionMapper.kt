package io.github.theestimator.rest.dto

import io.github.theestimator.domain.draft.DraftAdditionalCost
import io.github.theestimator.domain.draft.DraftEffortDriver
import io.github.theestimator.domain.draft.DraftEstimationNode
import io.github.theestimator.domain.draft.DraftEstimationParameter
import io.github.theestimator.domain.draft.DraftEstimationVersion
import io.github.theestimator.domain.draft.DraftFixedItemNode
import io.github.theestimator.domain.draft.DraftGroupNode
import io.github.theestimator.domain.draft.DraftProjectPhase
import io.github.theestimator.domain.draft.DraftTimeRelativeItemNode
import io.github.theestimator.domain.submitted.SubmittedAdditionalCost
import io.github.theestimator.domain.submitted.SubmittedEffortDriver
import io.github.theestimator.domain.submitted.SubmittedEstimationNode
import io.github.theestimator.domain.submitted.SubmittedEstimationParameter
import io.github.theestimator.domain.submitted.SubmittedEstimationVersion
import io.github.theestimator.domain.submitted.SubmittedGroupNode
import io.github.theestimator.domain.submitted.SubmittedProjectPhase
import io.github.theestimator.domain.submitted.SubmittedTimeRelativeItemNode
import io.github.theestimator.model.EstimationGroup
import io.github.theestimator.model.EstimationItem
import io.github.theestimator.model.EstimationNode
import io.github.theestimator.model.EstimationVersion

fun SubmittedEstimationVersion.toSummaryDto() = EstimationVersionSummaryDto(
    versionNumber = versionNumber,
    isDraft = false,
    totalEffort = totalEffort,
    notes = notes,
    createdAt = createdAt
)

fun DraftEstimationVersion.toSummaryDto(totalEffort: Double?) = EstimationVersionSummaryDto(
    versionNumber = versionNumber,
    isDraft = true,
    totalEffort = totalEffort,
    notes = notes,
    createdAt = createdAt
)

fun SubmittedEstimationVersion.toDto() = EstimationVersionDto(
    versionNumber = versionNumber,
    isDraft = false,
    totalEffort = totalEffort,
    notes = notes,
    createdAt = createdAt,
    submittedAt = submittedAt,
    parameters = parameters.map { it.toDto() },
    effortDrivers = effortDrivers.map { it.toDto() },
    phases = phases.map { it.toDto() },
    roots = roots.map { it.toDto() },
    additionalCosts = additionalCosts.map { it.toDto() }
)

fun DraftEstimationVersion.toDto(calculated: EstimationVersion): EstimationVersionDto {
    val calcMap: Map<String, EstimationNode> = collectNodes(calculated.roots).associateBy { it.logicalId }
    return EstimationVersionDto(
        versionNumber = versionNumber,
        isDraft = true,
        totalEffort = calculated.totalEffort,
        notes = notes,
        createdAt = createdAt,
        submittedAt = null,
        parameters = parameters.map { it.toDto() },
        effortDrivers = effortDrivers.map { it.toDto() },
        phases = phases.map { it.toDto() },
        roots = roots.map { it.toDtoWithCalc(calcMap) },
        additionalCosts = additionalCosts.map { it.toDto() }
    )
}

private fun collectNodes(nodes: List<EstimationNode>): List<EstimationNode> =
    nodes.flatMap { node ->
        when (node) {
            is EstimationGroup -> listOf(node) + collectNodes(node.children)
            else -> listOf(node)
        }
    }

fun DraftEstimationParameter.toDto() = EstimationParameterDto(
    id = id,
    name = name,
    value = value,
    comment = comment
)

fun DraftEffortDriver.toDto() = EffortDriverDto(
    id = id,
    description = description,
    factor = factor,
    comment = comment
)

fun DraftProjectPhase.toDto() = ProjectPhaseDto(
    id = id,
    name = name,
    abbreviation = abbreviation,
    durationWeeks = durationWeeks
)

fun DraftEstimationNode.toDtoWithCalc(calcMap: Map<String, EstimationNode>): EstimationNodeDto {
    val calc = calcMap[logicalId.toString()]
    return when (this) {
        is DraftGroupNode -> EstimationNodeDto(
            logicalId = logicalId,
            type = "GROUP",
            title = title,
            description = null,
            code = null,
            minEffort = null,
            expectedEffort = null,
            maxEffort = null,
            assumptions = null,
            mean = calc?.mean ?: 0.0,
            variance = calc?.variance ?: 0.0,
            riskSurcharge = calc?.riskSurcharge ?: 0.0,
            driverSurcharge = calc?.driverSurcharge ?: 0.0,
            offerPT = calc?.offerPT ?: 0.0,
            cost = calc?.cost ?: 0.0,
            offerPrice = calc?.offerPrice ?: 0.0,
            unit = null,
            phaseAbbreviation = null,
            children = children.map { it.toDtoWithCalc(calcMap) }
        )
        is DraftTimeRelativeItemNode -> leafDto(calc, "TIME_RELATIVE", unit)
        is DraftFixedItemNode -> leafDto(calc, "FIXED", null)
        else -> error("Unknown node type: ${this::class.simpleName}")
    }
}

private fun DraftEstimationNode.leafDto(calc: EstimationNode?, type: String, unitValue: String?) = EstimationNodeDto(
    logicalId = logicalId,
    type = type,
    title = null,
    description = description,
    code = code,
    minEffort = minEffort ?: 0.0,
    expectedEffort = expectedEffort ?: 0.0,
    maxEffort = maxEffort ?: 0.0,
    assumptions = assumptions,
    mean = calc?.mean ?: 0.0,
    variance = calc?.variance ?: 0.0,
    riskSurcharge = calc?.riskSurcharge ?: 0.0,
    driverSurcharge = calc?.driverSurcharge ?: 0.0,
    offerPT = calc?.offerPT ?: 0.0,
    cost = calc?.cost ?: 0.0,
    offerPrice = calc?.offerPrice ?: 0.0,
    unit = unitValue,
    phaseAbbreviation = phase?.abbreviation,
    children = emptyList()
)

fun DraftAdditionalCost.toDto() = AdditionalCostDto(
    id = id,
    description = description,
    amount = amount,
    type = type,
    amountPerWeek = amountPerWeek,
    phaseAbbreviation = phase?.abbreviation
)

fun SubmittedEstimationParameter.toDto() = EstimationParameterDto(
    id = id,
    name = name,
    value = value,
    comment = comment
)

fun SubmittedEffortDriver.toDto() = EffortDriverDto(
    id = id,
    description = description,
    factor = factor,
    comment = comment
)

fun SubmittedProjectPhase.toDto() = ProjectPhaseDto(
    id = id,
    name = name,
    abbreviation = abbreviation,
    durationWeeks = durationWeeks
)

fun SubmittedEstimationNode.toDto(): EstimationNodeDto = when (this) {
    is SubmittedGroupNode -> EstimationNodeDto(
        logicalId = logicalId,
        type = "GROUP",
        title = title,
        description = null,
        code = null,
        minEffort = null,
        expectedEffort = null,
        maxEffort = null,
        assumptions = null,
        mean = mean,
        variance = variance,
        riskSurcharge = riskSurcharge,
        driverSurcharge = driverSurcharge,
        offerPT = offerPT,
        cost = cost,
        offerPrice = offerPrice,
        unit = null,
        phaseAbbreviation = null,
        children = children.map { it.toDto() }
    )
    else -> EstimationNodeDto(
        logicalId = logicalId,
        type = if (this is SubmittedTimeRelativeItemNode) "TIME_RELATIVE" else "FIXED",
        title = null,
        description = description,
        code = code,
        minEffort = minEffort,
        expectedEffort = expectedEffort,
        maxEffort = maxEffort,
        assumptions = assumptions,
        mean = mean,
        variance = variance,
        riskSurcharge = riskSurcharge,
        driverSurcharge = driverSurcharge,
        offerPT = offerPT,
        cost = cost,
        offerPrice = offerPrice,
        unit = if (this is SubmittedTimeRelativeItemNode) unit else null,
        phaseAbbreviation = phaseAbbreviation,
        children = emptyList()
    )
}

fun SubmittedAdditionalCost.toDto() = AdditionalCostDto(
    id = id,
    description = description,
    amount = amount,
    type = type,
    amountPerWeek = amountPerWeek,
    phaseAbbreviation = phaseAbbreviation
)
