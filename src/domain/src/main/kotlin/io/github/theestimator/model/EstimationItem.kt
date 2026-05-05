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

    var calculationParameters: CalculationParameters = CalculationParameters()

    val mean: Double
        get() = PertCalculation.mean(minEffort ?: 0.0, expectedEffort ?: 0.0, maxEffort ?: 0.0)

    val variance: Double
        get() = PertCalculation.variance(minEffort ?: 0.0, maxEffort ?: 0.0)

    val riskSurcharge: Double
        get() = mean * calculationParameters.riskFactor

    val driverSurcharge: Double
        get() = mean * calculationParameters.totalDriverFactor

    val offerPT: Double
        get() = mean + riskSurcharge + driverSurcharge

    val cost: Double
        get() = offerPT * calculationParameters.dailyRate

    val offerPrice: Double
        get() = cost * (1 + calculationParameters.salesSurcharge)
}
