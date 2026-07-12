package io.github.theestimator.rest.dto

import io.github.theestimator.domain.ProjectStatus
import io.github.theestimator.method.EstimationMethod
import java.time.Instant
import java.util.UUID

data class ProjectSummaryDto(
    val id: UUID?,
    val name: String?,
    val description: String?,
    val client: String?,
    val status: ProjectStatus,
    val createdAt: Instant?
)

data class ProjectDetailDto(
    val id: UUID?,
    val name: String?,
    val description: String?,
    val client: String?,
    val status: ProjectStatus,
    val createdAt: Instant?,
    val estimations: List<EstimationSummaryDto>
)

data class EstimationSummaryDto(
    val id: UUID?,
    val offer: String?,
    val description: String?,
    val method: EstimationMethod,
    val latestVersionNumber: Int?,
    val versionCount: Int,
    val hasDraft: Boolean,
    val createdAt: Instant?
)

data class ProjectCreateDto(
    val name: String,
    val description: String? = null,
    val client: String? = null
)

data class ProjectUpdateDto(
    val name: String? = null,
    val description: String? = null,
    val client: String? = null
)

data class EstimationCreateDto(
    val offer: String,
    val description: String? = null,
    val method: EstimationMethod = EstimationMethod.THREE_POINT_PERT
)

data class EstimationDetailDto(
    val id: UUID?,
    val offer: String?,
    val description: String?,
    val method: EstimationMethod,
    val projectId: UUID?,
    val projectName: String?,
    val latestVersionNumber: Int?,
    val hasDraft: Boolean,
    val createdAt: Instant?,
    val versions: List<EstimationVersionSummaryDto>
)
