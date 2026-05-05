package io.github.theestimator.rest.dto

import io.github.theestimator.domain.ProjectStatus
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
    val currentVersionNumber: Int?,
    val versionCount: Int,
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
