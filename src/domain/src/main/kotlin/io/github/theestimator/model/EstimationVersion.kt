@file:OptIn(ExperimentalJsExport::class)

package io.github.theestimator.model

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

private val logger = KotlinLogging.logger {}

@JsExport
@DomainEntity
data class EstimationVersion(
    val versionNumber: Int,
    val status: EstimationVersionStatus = EstimationVersionStatus.DRAFT,
    val createdBy: User? = null,
    val totalEffort: Double = 0.0,
    val notes: String = "",
    val parameters: List<EstimationParameter> = emptyList(),
    val effortDrivers: List<EffortDriver> = emptyList(),
    val phases: List<ProjectPhase> = emptyList(),
    val additionalCosts: List<AdditionalCost> = emptyList(),
    val roots: List<EstimationNode> = emptyList(),
    private val _id: String? = null,
    private val _createdAt: String? = null,
    private val _updatedAt: String? = null
) : BaseDomain(_id, _createdAt, _updatedAt) {

    fun parameterValue(name: String): Double? =
        parameters.find { it.name == name }?.value

    fun calculate(): EstimationVersion {
        val stdDevFactor = parameterValue("Standardabweichungsfaktor") ?: 2.0
        val dailyRate = parameterValue("Tagessatz") ?: 800.0
        val salesSurcharge = parameterValue("Vertriebszuschlag") ?: 0.1
        val totalDriverFactor = effortDrivers.sumOf { it.factor }

        val leaves = roots.flatMap { it.leaves().toList() }

        val totalVariance = leaves.sumOf { it.variance }
        val totalMean = leaves.sumOf { it.mean }
        val riskFactor = PertCalculation.riskFactor(totalMean, totalVariance, stdDevFactor)

        val params = CalculationParameters(riskFactor, totalDriverFactor, dailyRate, salesSurcharge)

        val newRoots = roots.map { it.withCalculationParameters(params) }
        val newTotalEffort = newRoots.sumOf { it.offerPT }

        logger.debug { "calculate(): ${leaves.size} leaves, totalMean=$totalMean, totalEffort=$newTotalEffort" }

        return copy(roots = newRoots, totalEffort = newTotalEffort)
    }
}
