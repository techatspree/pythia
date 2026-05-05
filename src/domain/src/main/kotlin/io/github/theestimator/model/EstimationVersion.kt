package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

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
    val itemGroups: List<EstimationItemGroup> = emptyList(),
    private val _id: UUID? = null,
    private val _createdAt: Instant? = null,
    private val _updatedAt: Instant? = null
) : BaseDomain(_id, _createdAt, _updatedAt) {

    fun parameterValue(name: String): Double? =
        parameters.find { it.name == name }?.value

    fun calculate(): EstimationVersion {
        val stdDevFactor = parameterValue("Standardabweichungsfaktor") ?: 2.0
        val dailyRate = parameterValue("Tagessatz") ?: 800.0
        val salesSurcharge = parameterValue("Vertriebszuschlag") ?: 0.1
        val totalDriverFactor = effortDrivers.sumOf { it.factor }

        val allItems = itemGroups.flatMap { it.items }

        val totalVariance = allItems.sumOf { it.variance }
        val totalMean = allItems.sumOf { it.mean }
        val riskFactor = PertCalculation.riskFactor(totalMean, totalVariance, stdDevFactor)

        val params = CalculationParameters(riskFactor, totalDriverFactor, dailyRate, salesSurcharge)

        val newGroups = itemGroups.map { group ->
            group.copy(items = group.items.map { it.withCalculationParameters(params) })
        }

        val newTotalEffort = newGroups.flatMap { it.items }.sumOf { it.offerPT }

        return copy(itemGroups = newGroups, totalEffort = newTotalEffort)
    }
}
