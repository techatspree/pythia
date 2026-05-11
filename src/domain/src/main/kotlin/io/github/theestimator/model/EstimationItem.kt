@file:OptIn(ExperimentalJsExport::class)

package io.github.theestimator.model

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@JsExport
@DomainEntity
sealed class EstimationItem(
    val description: String,
    val code: String = "",
    val minEffort: Double = 0.0,
    val expectedEffort: Double = 0.0,
    val maxEffort: Double = 0.0,
    val assumptions: String = "",
    val phase: ProjectPhase? = null,
    val logicalId: String = newId(),
    val calculationParameters: CalculationParameters = CalculationParameters(),
    id: String? = null,
    createdAt: String? = null,
    updatedAt: String? = null
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
