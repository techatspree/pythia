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

    fun calculateMeanAndVariance() {
        val min = minEffort ?: 0.0
        val expected = expectedEffort ?: 0.0
        val max = maxEffort ?: 0.0
        mean = PertCalculation.mean(min, expected, max)
        variance = PertCalculation.variance(min, max)
    }

    fun calculateDerived(riskFactor: Double, totalDriverFactor: Double, dailyRate: Double, salesSurcharge: Double) {
        val m = mean ?: 0.0
        riskSurcharge = m * riskFactor
        driverSurcharge = m * totalDriverFactor
        offerPT = m + (riskSurcharge ?: 0.0) + (driverSurcharge ?: 0.0)
        cost = (offerPT ?: 0.0) * dailyRate
        offerPrice = (cost ?: 0.0) * (1 + salesSurcharge)
    }
}
