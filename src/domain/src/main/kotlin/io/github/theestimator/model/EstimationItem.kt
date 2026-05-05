package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
sealed class EstimationItem(
    val description: String,
    val code: String = "",
    val minEffort: Double = 0.0,
    val expectedEffort: Double = 0.0,
    val maxEffort: Double = 0.0,
    val assumptions: String = "",
    val phase: ProjectPhase? = null,
    val logicalId: UUID = UUID.randomUUID(),
    val calculationParameters: CalculationParameters = CalculationParameters(),
    id: UUID? = null,
    createdAt: Instant? = null,
    updatedAt: Instant? = null
) : BaseDomain(id, createdAt, updatedAt) {

    val mean: Double
        get() = PertCalculation.mean(minEffort, expectedEffort, maxEffort)

    val variance: Double
        get() = PertCalculation.variance(minEffort, maxEffort)

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
