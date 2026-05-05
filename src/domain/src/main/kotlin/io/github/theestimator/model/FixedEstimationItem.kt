package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
class FixedEstimationItem(
    id: UUID? = null,
    createdAt: Instant? = null
) : EstimationItem(id, createdAt)
