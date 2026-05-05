package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
class Estimation(
    id: UUID? = null,
    createdAt: Instant? = null
) : BaseDomain(id, createdAt) {
    var offer: String? = null
    var description: String? = null
    var project: Project? = null
    var currentVersion: EstimationVersion? = null
    var versions: MutableList<EstimationVersion> = mutableListOf()
}
