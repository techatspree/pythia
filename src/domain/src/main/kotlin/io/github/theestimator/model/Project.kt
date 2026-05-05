package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
data class Project(
    val name: String? = null,
    val description: String? = null,
    val client: String? = null,
    val status: ProjectStatus = ProjectStatus.ACTIVE,
    val owner: User? = null,
    private val _id: UUID? = null,
    private val _createdAt: Instant? = null,
    private val _updatedAt: Instant? = null
) : BaseDomain(_id, _createdAt, _updatedAt)
