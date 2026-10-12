package io.pythia.rest.dto

import io.pythia.domain.Estimation
import io.pythia.domain.Project
import io.pythia.domain.User
import io.pythia.method.EstimationMethodRegistry

fun User.toRefDto() = UserRefDto(id = id, displayName = displayName)

fun Project.toSummaryDto(canChangeOwner: Boolean) = ProjectSummaryDto(
    id = id,
    name = name,
    description = description,
    client = client,
    status = status,
    createdAt = createdAt,
    owner = owner?.toRefDto(),
    canChangeOwner = canChangeOwner
)

fun Project.toDetailDto(canChangeOwner: Boolean) = ProjectDetailDto(
    id = id,
    name = name,
    description = description,
    client = client,
    status = status,
    createdAt = createdAt,
    owner = owner?.toRefDto(),
    canChangeOwner = canChangeOwner,
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
        methodDescription = EstimationMethodRegistry.require(method).description,
        projectId = project?.id,
        projectName = project?.name,
        latestVersionNumber = latestSubmittedVersion?.versionNumber,
        hasDraft = draftVersion != null,
        createdAt = createdAt,
        versions = versionList,
        buckets = buckets.map { EstimationBucketDto(id = it.id, position = it.position, label = it.label) }
    )
}
