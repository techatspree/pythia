@file:OptIn(ExperimentalJsExport::class)

package io.github.theestimator.model

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

// The constructor parameters are the leaf entity's fields; this is an
// @JsExport @DomainEntity whose shape is part of the public domain API.
@Suppress("LongParameterList")
@JsExport
@DomainEntity
abstract class EstimationItem(
    val description: String,
    val code: String = "",
    val minEffort: Double = 0.0,
    val expectedEffort: Double = 0.0,
    val maxEffort: Double = 0.0,
    val assumptions: String = "",
    val phase: ProjectPhase? = null,
    logicalId: String = newId(),
    val calculationParameters: CalculationParameters = CalculationParameters(),
    id: String? = null,
    createdAt: String? = null,
    updatedAt: String? = null
) : EstimationNode(logicalId, id, createdAt, updatedAt) {

    override val mean: Double
        get() = PertCalculation.mean(minEffort, expectedEffort, maxEffort)

    override val variance: Double
        get() = PertCalculation.variance(minEffort, maxEffort)

    override val riskSurcharge: Double
        get() = mean * calculationParameters.riskFactor

    override val driverSurcharge: Double
        get() = mean * calculationParameters.totalDriverFactor

    override val offerPT: Double
        get() = mean + riskSurcharge + driverSurcharge

    override val cost: Double
        get() = offerPT * calculationParameters.dailyRate

    override val offerPrice: Double
        get() = cost * (1 + calculationParameters.salesSurcharge)

    abstract override fun withCalculationParameters(params: CalculationParameters): EstimationItem
}
