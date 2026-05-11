package io.github.theestimator.rest.dto

import io.github.theestimator.domain.draft.*
import io.github.theestimator.domain.submitted.*
import io.github.theestimator.model.EstimationItem
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
    itemGroups = itemGroups.map { it.toDto() },
    additionalCosts = additionalCosts.map { it.toDto() }
)

fun DraftEstimationVersion.toDto(calculated: EstimationVersion) = EstimationVersionDto(
    versionNumber = versionNumber,
    isDraft = true,
    totalEffort = calculated.totalEffort,
    notes = notes,
    createdAt = createdAt,
    submittedAt = null,
    parameters = parameters.map { it.toDto() },
    effortDrivers = effortDrivers.map { it.toDto() },
    phases = phases.map { it.toDto() },
    itemGroups = itemGroups.map { it.toDtoWithCalc(calculated) },
    additionalCosts = additionalCosts.map { it.toDto() }
)

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

fun DraftEstimationItemGroup.toDtoWithCalc(calculated: EstimationVersion): EstimationItemGroupDto {
    val itemResultMap = calculated.itemGroups.flatMap { it.items }.associateBy { it.logicalId }
    return EstimationItemGroupDto(
        logicalId = logicalId,
        title = title,
        phaseAbbreviation = phase?.abbreviation,
        items = items.map { it.toDto(itemResultMap[it.logicalId.toString()]) }
    )
}

fun DraftEstimationItem.toDto(calc: EstimationItem?): EstimationItemDto = EstimationItemDto(
    logicalId = logicalId,
    type = when (this) {
        is DraftTimeRelativeEstimationItem -> "TIME_RELATIVE"
        else -> "FIXED"
    },
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
    unit = if (this is DraftTimeRelativeEstimationItem) unit else null
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

fun SubmittedEstimationItemGroup.toDto() = EstimationItemGroupDto(
    logicalId = logicalId,
    title = title,
    phaseAbbreviation = phaseAbbreviation,
    items = items.map { it.toDto() }
)

fun SubmittedEstimationItem.toDto() = EstimationItemDto(
    logicalId = logicalId,
    type = when (this) {
        is SubmittedTimeRelativeEstimationItem -> "TIME_RELATIVE"
        else -> "FIXED"
    },
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
    unit = if (this is SubmittedTimeRelativeEstimationItem) unit else null
)

fun SubmittedAdditionalCost.toDto() = AdditionalCostDto(
    id = id,
    description = description,
    amount = amount,
    type = type,
    amountPerWeek = amountPerWeek,
    phaseAbbreviation = phaseAbbreviation
)
