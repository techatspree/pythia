package io.pythia.rest.dto

import io.pythia.domain.ProjectStatus
import io.pythia.method.EstimationMethod
import java.time.Instant
import java.util.UUID

data class ProjectSummaryDto(
    val id: UUID?,
    val name: String?,
    val description: String?,
    val client: String?,
    val status: ProjectStatus,
    val createdAt: Instant?,
    // The project's owner (task-200); null for a project from before ownership existed.
    val owner: UserRefDto?,
    // Whether the CURRENT user may transfer ownership (owner or ADMIN). The UI only
    // shows or hides its control by this; PUT /owner enforces the rule itself.
    val canChangeOwner: Boolean
)

data class ProjectDetailDto(
    val id: UUID?,
    val name: String?,
    val description: String?,
    val client: String?,
    val status: ProjectStatus,
    val createdAt: Instant?,
    val owner: UserRefDto?,
    val canChangeOwner: Boolean,
    val estimations: List<EstimationSummaryDto>
)

/** A user as referenced from other resources: id plus display name (task-200). */
data class UserRefDto(
    val id: UUID?,
    val displayName: String?
)

/** Body of PUT /api/projects/{id}/owner (task-200). */
data class ProjectOwnerUpdateDto(
    val ownerId: UUID
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
    // Human-readable English description of `method`, sourced from
    // EstimationMethodModule.description (task-119). Rendered verbatim in the
    // frontend method-detail popover.
    val methodDescription: String,
    val projectId: UUID?,
    val projectName: String?,
    val latestVersionNumber: Int?,
    val hasDraft: Boolean,
    val createdAt: Instant?,
    val versions: List<EstimationVersionSummaryDto>,
    // Buckets of the bucket + sampled method (task-103); empty for PERT.
    val buckets: List<EstimationBucketDto> = emptyList()
)

data class EstimationBucketDto(
    val id: UUID?,
    val position: Int,
    val label: String
)
