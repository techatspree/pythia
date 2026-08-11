@file:OptIn(ExperimentalJsExport::class)

package io.pythia.model

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.math.sqrt

@JsExport
object PertCalculation {

    /** Weight applied to the "most likely" estimate in the PERT mean formula. */
    private const val LIKELY_WEIGHT = 4.0

    /** PERT divisor: (optimistic + 4·likely + pessimistic) / six. */
    private const val PERT_DIVISOR = 6.0

    fun mean(min: Double, expected: Double, max: Double): Double =
        (min + LIKELY_WEIGHT * expected + max) / PERT_DIVISOR

    fun variance(min: Double, max: Double): Double {
        val range = (max - min) / PERT_DIVISOR
        return range * range
    }

    fun riskFactor(totalMean: Double, totalVariance: Double, stdDevFactor: Double): Double {
        if (totalMean <= 0) return 0.0
        return (sqrt(totalVariance) * stdDevFactor) / totalMean
    }
}
