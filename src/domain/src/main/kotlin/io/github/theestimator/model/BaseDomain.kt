package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
abstract class BaseDomain(
    val id: UUID? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)
