package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
class EstimationParameter(
    id: UUID? = null,
    createdAt: Instant? = null
) : BaseDomain(id, createdAt) {
    var name: String? = null
    var value: Double = 0.0
    var comment: String? = null
    var version: EstimationVersion? = null
}
