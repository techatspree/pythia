package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
abstract class EstimationItem(
    id: UUID? = null,
    createdAt: Instant? = null
) : BaseDomain(id, createdAt) {
    var description: String? = null
    var code: String? = null
    var minEffort: Double? = null
    var expectedEffort: Double? = null
    var maxEffort: Double? = null
    var assumptions: String? = null
    var phase: ProjectPhase? = null
    var group: EstimationItemGroup? = null

    // Derived/calculated fields
    var mean: Double? = null
    var variance: Double? = null
    var riskSurcharge: Double? = null
    var driverSurcharge: Double? = null
    var offerPT: Double? = null
    var cost: Double? = null
    var offerPrice: Double? = null
}
