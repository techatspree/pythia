package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
abstract class BaseDomain {
    var id: UUID? = null
    var createdAt: Instant? = null
    var updatedAt: Instant? = null
}
