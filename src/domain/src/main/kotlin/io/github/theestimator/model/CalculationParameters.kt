package io.github.theestimator.model

data class CalculationParameters(
    val riskFactor: Double = 0.0,
    val totalDriverFactor: Double = 0.0,
    val dailyRate: Double = 0.0,
    val salesSurcharge: Double = 0.0
)
