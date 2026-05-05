package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
class EffortDriver(
    id: UUID? = null,
    createdAt: Instant? = null
) : BaseDomain(id, createdAt) {
    var description: String? = null
    var factor: Double = 0.0
    var comment: String? = null
    var version: EstimationVersion? = null
}
