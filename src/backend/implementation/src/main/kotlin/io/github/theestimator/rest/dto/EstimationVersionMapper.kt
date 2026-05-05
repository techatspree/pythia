package io.github.theestimator.rest.dto

import io.github.theestimator.domain.*

fun EstimationVersion.toSummaryDto() = EstimationVersionSummaryDto(
    id = id,
    versionNumber = versionNumber,
    status = status,
    totalEffort = totalEffort,
    notes = notes,
    createdAt = createdAt
)

fun EstimationVersion.toDto() = EstimationVersionDto(
    id = id,
    versionNumber = versionNumber,
    status = status,
    totalEffort = totalEffort,
    notes = notes,
    createdAt = createdAt,
    parameters = parameters.map { it.toDto() },
    effortDrivers = effortDrivers.map { it.toDto() },
    phases = phases.map { it.toDto() },
    itemGroups = itemGroups.map { it.toDto() },
    additionalCosts = additionalCosts.map { it.toDto() }
)

fun EstimationParameter.toDto() = EstimationParameterDto(
    id = id,
    name = name,
    value = value,
    comment = comment
)

fun EffortDriver.toDto() = EffortDriverDto(
    id = id,
    description = description,
    factor = factor,
    comment = comment
)

fun ProjectPhase.toDto() = ProjectPhaseDto(
    id = id,
    name = name,
    abbreviation = abbreviation,
    durationWeeks = durationWeeks
)

fun EstimationItemGroup.toDto() = EstimationItemGroupDto(
    id = id,
    title = title,
    phaseAbbreviation = phase?.abbreviation,
    items = items.map { it.toDto() }
)

fun EstimationItem.toDto() = EstimationItemDto(
    id = id,
    type = when (this) {
        is TimeRelativeEstimationItem -> "TIME_RELATIVE"
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
    unit = if (this is TimeRelativeEstimationItem) unit else null
)

fun AdditionalCost.toDto() = AdditionalCostDto(
    id = id,
    description = description,
    amount = amount,
    type = type,
    amountPerWeek = amountPerWeek,
    phaseAbbreviation = phase?.abbreviation
)
