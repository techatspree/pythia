package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
data class EstimationParameter(
    val name: String? = null,
    val value: Double = 0.0,
    val comment: String? = null,
    private val _id: UUID? = null,
    private val _createdAt: Instant? = null,
    private val _updatedAt: Instant? = null
) : BaseDomain(_id, _createdAt, _updatedAt)
