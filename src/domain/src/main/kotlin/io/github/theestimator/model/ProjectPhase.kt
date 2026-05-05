package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
data class ProjectPhase(
    val name: String,
    val abbreviation: String,
    val durationWeeks: Double = 0.0,
    private val _id: UUID? = null,
    private val _createdAt: Instant? = null,
    private val _updatedAt: Instant? = null
) : BaseDomain(_id, _createdAt, _updatedAt)
