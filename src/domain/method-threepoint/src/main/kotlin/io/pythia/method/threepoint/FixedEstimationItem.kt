@file:OptIn(ExperimentalJsExport::class)

package io.pythia.method.threepoint

import io.pythia.method.EstimationMethod
import io.pythia.model.CalculationParameters
import io.pythia.model.DomainEntity
import io.pythia.model.EstimationItem
import io.pythia.model.ProjectPhase
import io.pythia.model.newId
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@JsExport
@DomainEntity
data class FixedEstimationItem(
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
) : EstimationItem(
    _description,
    _code,
    _minEffort,
    _expectedEffort,
    _maxEffort,
    _assumptions,
    _phase,
    _logicalId,
    _calculationParameters,
    _id,
    _createdAt,
    _updatedAt
) {

    override val method: EstimationMethod = EstimationMethod.THREE_POINT_PERT

    override val nodeTypeLabel: String = "FIXED"

    // The inherited diffFields() default — optimistic / likely / pessimistic /
    // assumptions / phase — is exactly this leaf's shape.

    override fun withCalculationParameters(params: CalculationParameters): FixedEstimationItem =
        copy(_calculationParameters = params)
}
