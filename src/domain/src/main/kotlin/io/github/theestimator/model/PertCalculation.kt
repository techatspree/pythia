package io.github.theestimator.model

import kotlin.math.sqrt

object PertCalculation {

    fun mean(min: Double, expected: Double, max: Double): Double =
        (min + 4 * expected + max) / 6.0

    fun variance(min: Double, max: Double): Double {
        val range = (max - min) / 6.0
        return range * range
    }

    fun riskFactor(totalMean: Double, totalVariance: Double, stdDevFactor: Double): Double {
        if (totalMean <= 0) return 0.0
        return (sqrt(totalVariance) * stdDevFactor) / totalMean
    }
}
