package io.github.theestimator.rest.dto

import io.github.theestimator.domain.draft.*
import io.github.theestimator.domain.submitted.*
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

@Suppress("DEPRECATION")
fun SubmittedEstimationVersion.toDto(): EstimationVersionDto {
    val rootDtos = roots.map { it.toDto() }
    return EstimationVersionDto(
        versionNumber = versionNumber,
        isDraft = false,
        totalEffort = totalEffort,
        notes = notes,
        createdAt = createdAt,
        submittedAt = submittedAt,
        parameters = parameters.map { it.toDto() },
        effortDrivers = effortDrivers.map { it.toDto() },
        phases = phases.map { it.toDto() },
        roots = rootDtos,
        additionalCosts = additionalCosts.map { it.toDto() },
        itemGroups = legacyItemGroups(rootDtos)
    )
}

@Suppress("DEPRECATION")
fun DraftEstimationVersion.toDto(calculated: EstimationVersion): EstimationVersionDto {
    val calcMap: Map<String, EstimationNode> = collectNodes(calculated.roots).associateBy { it.logicalId }
    val rootDtos = roots.map { it.toDtoWithCalc(calcMap) }
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
        roots = rootDtos,
        additionalCosts = additionalCosts.map { it.toDto() },
        itemGroups = legacyItemGroups(rootDtos)
    )
}

// task-054 compat shim: flatten the recursive tree into the legacy
// depth-1 [{title, items}] shape so the unmodified frontend renders.
// Only GROUP roots become groups; their LEAF descendants (at any depth)
// collapse into the group's items list. Removed in task-054.
@Suppress("DEPRECATION")
private fun legacyItemGroups(roots: List<EstimationNodeDto>): List<LegacyItemGroupDto> =
    roots.filter { it.type == "GROUP" }.map { group ->
        LegacyItemGroupDto(
            logicalId = group.logicalId,
            title = group.title ?: "",
            items = collectLegacyItems(group.children)
        )
    }

@Suppress("DEPRECATION")
private fun collectLegacyItems(nodes: List<EstimationNodeDto>): List<LegacyItemDto> =
    nodes.flatMap { node ->
        if (node.type == "GROUP") collectLegacyItems(node.children)
        else listOf(LegacyItemDto(
            logicalId = node.logicalId,
            type = node.type,
            description = node.description ?: "",
            code = node.code,
            minEffort = node.minEffort ?: 0.0,
            expectedEffort = node.expectedEffort ?: 0.0,
            maxEffort = node.maxEffort ?: 0.0,
            assumptions = node.assumptions,
            mean = node.mean,
            variance = node.variance,
            riskSurcharge = node.riskSurcharge,
            driverSurcharge = node.driverSurcharge,
            offerPT = node.offerPT,
            cost = node.cost,
            offerPrice = node.offerPrice,
            unit = node.unit,
            phaseAbbreviation = node.phaseAbbreviation
        ))
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
