package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
data class FixedEstimationItem(
    private val _description: String? = null,
    private val _code: String? = null,
    private val _minEffort: Double? = null,
    private val _expectedEffort: Double? = null,
    private val _maxEffort: Double? = null,
    private val _assumptions: String? = null,
    private val _phase: ProjectPhase? = null,
    private val _calculationParameters: CalculationParameters = CalculationParameters(),
    private val _id: UUID? = null,
    private val _createdAt: Instant? = null,
    private val _updatedAt: Instant? = null
) : EstimationItem(_description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _calculationParameters, _id, _createdAt, _updatedAt) {

    override fun withCalculationParameters(params: CalculationParameters): FixedEstimationItem =
        copy(_calculationParameters = params)
}
