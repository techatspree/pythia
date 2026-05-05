package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
abstract class BaseDomain(
    var id: UUID?,
    var createdAt: Instant?,
    var updatedAt: Instant? = null
)
