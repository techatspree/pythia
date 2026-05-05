package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
class ProjectPhase(
    id: UUID? = null,
    createdAt: Instant? = null
) : BaseDomain(id, createdAt) {
    var name: String? = null
    var abbreviation: String? = null
    var durationWeeks: Double? = null
    var version: EstimationVersion? = null
}
