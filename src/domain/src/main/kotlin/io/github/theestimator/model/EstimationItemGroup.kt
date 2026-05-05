package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
class EstimationItemGroup(
    id: UUID? = null,
    createdAt: Instant? = null
) : BaseDomain(id, createdAt) {
    var title: String? = null
    var phase: ProjectPhase? = null
    var version: EstimationVersion? = null
    var items: MutableList<EstimationItem> = mutableListOf()
}
