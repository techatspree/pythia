@file:OptIn(ExperimentalJsExport::class)

package io.pythia.model

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@JsExport
data class CalculationParameters(
    val riskFactor: Double = 0.0,
    val totalDriverFactor: Double = 0.0,
    val dailyRate: Double = 0.0,
    val salesSurcharge: Double = 0.0,
    /**
     * Hours of one working day, the divisor that turns accompanying work's
     * hours into person-days (task-213). A tenant setting, passed in by the
     * caller through [EstimationVersion.hoursPerDay].
     */
    val hoursPerDay: Double = EstimationDefaults.HOURS_PER_DAY
)
