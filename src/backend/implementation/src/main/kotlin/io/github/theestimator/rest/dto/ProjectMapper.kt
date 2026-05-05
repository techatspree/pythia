package io.github.theestimator.rest.dto

import io.github.theestimator.domain.Estimation
import io.github.theestimator.domain.Project

fun Project.toSummaryDto() = ProjectSummaryDto(
    id = id,
    name = name,
    description = description,
    client = client,
    status = status,
    createdAt = createdAt
)

fun Project.toDetailDto() = ProjectDetailDto(
    id = id,
    name = name,
    description = description,
    client = client,
    status = status,
    createdAt = createdAt,
    estimations = estimations.map { it.toSummaryDto() }
)

fun Estimation.toSummaryDto() = EstimationSummaryDto(
    id = id,
    offer = offer,
    description = description,
    currentVersionNumber = currentVersion?.versionNumber,
    versionCount = versions.size,
    createdAt = createdAt
)
