package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
sealed class EstimationItem(
    val description: String? = null,
    val code: String? = null,
    val minEffort: Double? = null,
    val expectedEffort: Double? = null,
    val maxEffort: Double? = null,
    val assumptions: String? = null,
    val phase: ProjectPhase? = null,
    val calculationParameters: CalculationParameters = CalculationParameters(),
    id: UUID? = null,
    createdAt: Instant? = null,
    updatedAt: Instant? = null
) : BaseDomain(id, createdAt, updatedAt) {

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

    abstract fun withCalculationParameters(params: CalculationParameters): EstimationItem
}
