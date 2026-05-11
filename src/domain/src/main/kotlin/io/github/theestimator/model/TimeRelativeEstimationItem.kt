@file:OptIn(ExperimentalJsExport::class)

package io.github.theestimator.model

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@JsExport
@DomainEntity
data class TimeRelativeEstimationItem(
    val unit: String = "h/Woche",
    private val _description: String,
    private val _code: String = "",
    private val _minEffort: Double = 0.0,
    private val _expectedEffort: Double = 0.0,
    private val _maxEffort: Double = 0.0,
    private val _assumptions: String = "",
    private val _phase: ProjectPhase? = null,
    private val _logicalId: String = newId(),
    private val _calculationParameters: CalculationParameters = CalculationParameters(),
    private val _id: String? = null,
    private val _createdAt: String? = null,
    private val _updatedAt: String? = null
) : EstimationItem(_description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt) {

    override fun withCalculationParameters(params: CalculationParameters): TimeRelativeEstimationItem =
        copy(_calculationParameters = params)
}
