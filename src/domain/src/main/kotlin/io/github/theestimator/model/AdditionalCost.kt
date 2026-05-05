package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
class AdditionalCost(
    id: UUID? = null,
    createdAt: Instant? = null
) : BaseDomain(id, createdAt) {
    var description: String? = null
    var amount: Double = 0.0
    var type: AdditionalCostType = AdditionalCostType.ONE_TIME
    var amountPerWeek: Double? = null
    var phase: ProjectPhase? = null
    var version: EstimationVersion? = null
}
