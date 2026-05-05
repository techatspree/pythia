package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
data class EstimationItemGroup(
    val title: String,
    val phase: ProjectPhase? = null,
    val items: List<EstimationItem> = emptyList(),
    private val _id: UUID? = null,
    private val _createdAt: Instant? = null,
    private val _updatedAt: Instant? = null
) : BaseDomain(_id, _createdAt, _updatedAt)
