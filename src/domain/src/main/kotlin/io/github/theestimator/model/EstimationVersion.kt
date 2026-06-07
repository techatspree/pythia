@file:OptIn(ExperimentalJsExport::class)

package io.github.theestimator.model

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.JsName

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

    @Deprecated("Use roots; removed when adapter.ts moves to roots in task-054")
    @JsName("createFromItemGroups")
    constructor(
        versionNumber: Int,
        status: EstimationVersionStatus = EstimationVersionStatus.DRAFT,
        createdBy: User? = null,
        totalEffort: Double = 0.0,
        notes: String = "",
        parameters: List<EstimationParameter> = emptyList(),
        effortDrivers: List<EffortDriver> = emptyList(),
        phases: List<ProjectPhase> = emptyList(),
        additionalCosts: List<AdditionalCost> = emptyList(),
        itemGroups: List<EstimationItemGroup>
    ) : this(
        versionNumber = versionNumber,
        status = status,
        createdBy = createdBy,
        totalEffort = totalEffort,
        notes = notes,
        parameters = parameters,
        effortDrivers = effortDrivers,
        phases = phases,
        additionalCosts = additionalCosts,
        roots = itemGroups.map { g ->
            EstimationGroup(title = g.title, children = g.items, _logicalId = g.logicalId)
        }
    )

    @Deprecated("Use roots; removed when adapter.ts moves to roots in task-054")
    val itemGroups: List<EstimationItemGroup>
        get() = roots.filterIsInstance<EstimationGroup>().map { g ->
            EstimationItemGroup(
                title = g.title,
                logicalId = g.logicalId,
                items = g.children.filterIsInstance<EstimationItem>()
            )
        }

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

        return copy(roots = newRoots, totalEffort = newTotalEffort)
    }
}
