package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
class EstimationVersion(
    id: UUID? = null,
    createdAt: Instant? = null
) : BaseDomain(id, createdAt) {
    var versionNumber: Int? = null
    var status: EstimationVersionStatus = EstimationVersionStatus.DRAFT
    var createdBy: User? = null
    var totalEffort: Double? = null
    var notes: String? = null
    var estimation: Estimation? = null
    var parameters: MutableList<EstimationParameter> = mutableListOf()
    var effortDrivers: MutableList<EffortDriver> = mutableListOf()
    var phases: MutableList<ProjectPhase> = mutableListOf()
    var additionalCosts: MutableList<AdditionalCost> = mutableListOf()
    var itemGroups: MutableList<EstimationItemGroup> = mutableListOf()

    fun parameterValue(name: String): Double? =
        parameters.find { it.name == name }?.value

    fun calculate() {
        val stdDevFactor = parameterValue("Standardabweichungsfaktor") ?: 2.0
        val dailyRate = parameterValue("Tagessatz") ?: 800.0
        val salesSurcharge = parameterValue("Vertriebszuschlag") ?: 0.1
        val totalDriverFactor = effortDrivers.sumOf { it.factor }

        val allItems = itemGroups.flatMap { it.items }

        val totalVariance = allItems.sumOf { it.variance }
        val totalMean = allItems.sumOf { it.mean }
        val riskFactor = PertCalculation.riskFactor(totalMean, totalVariance, stdDevFactor)

        val params = CalculationParameters(riskFactor, totalDriverFactor, dailyRate, salesSurcharge)
        allItems.forEach { it.calculationParameters = params }

        totalEffort = allItems.sumOf { it.offerPT }
    }
}
