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
    method = method,
    latestVersionNumber = latestSubmittedVersion?.versionNumber,
    versionCount = submittedVersions.size + (if (draftVersion != null) 1 else 0),
    hasDraft = draftVersion != null,
    createdAt = createdAt
)

fun Estimation.toEstimationDetailDto(draftTotalEffort: Double? = null): EstimationDetailDto {
    val versionList = mutableListOf<EstimationVersionSummaryDto>()
    draftVersion?.let { draft ->
        versionList.add(draft.toSummaryDto(draftTotalEffort))
    }
    versionList.addAll(
        submittedVersions
            .sortedByDescending { it.versionNumber }
            .map { it.toSummaryDto() }
    )
    return EstimationDetailDto(
        id = id,
        offer = offer,
        description = description,
        method = method,
        projectId = project?.id,
        projectName = project?.name,
        latestVersionNumber = latestSubmittedVersion?.versionNumber,
        hasDraft = draftVersion != null,
        createdAt = createdAt,
        versions = versionList
    )
}
