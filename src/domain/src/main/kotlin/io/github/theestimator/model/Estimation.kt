package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
data class Estimation(
    val offer: String = "",
    val description: String = "",
    val currentVersion: EstimationVersion? = null,
    val versions: List<EstimationVersion> = emptyList(),
    private val _id: UUID? = null,
    private val _createdAt: Instant? = null,
    private val _updatedAt: Instant? = null
) : BaseDomain(_id, _createdAt, _updatedAt)
