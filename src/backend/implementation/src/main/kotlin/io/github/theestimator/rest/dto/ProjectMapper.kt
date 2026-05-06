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
    versionCount = submittedVersions.size + (if (draftVersion != null) 1 else 0),
    hasDraft = draftVersion != null,
    createdAt = createdAt
)

fun Estimation.toEstimationDetailDto() = EstimationDetailDto(
    id = id,
    offer = offer,
    description = description,
    projectId = project?.id,
    projectName = project?.name,
    currentVersionNumber = currentVersion?.versionNumber,
    hasDraft = draftVersion != null,
    createdAt = createdAt,
    versions = submittedVersions
        .sortedByDescending { it.versionNumber }
        .map { it.toSummaryDto() }
)
