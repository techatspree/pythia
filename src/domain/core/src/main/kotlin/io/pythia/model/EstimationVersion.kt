@file:OptIn(ExperimentalJsExport::class)

package io.pythia.model

import io.pythia.method.EstimationMethod
import io.pythia.method.EstimationMethodRegistry
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
    // The three values the calculation depends on are FIRST-CLASS FIELDS, not
    // user-named rows (task-138). They were previously looked up by their
    // English names as string keys in a generic
    // name/value list that the GUI let users rename — a rename made the lookup
    // miss and SILENTLY fall back to the default, quietly recomputing the whole
    // estimate. Being fields, they cannot be renamed away; GUI labels are i18n
    // keys instead.
    val dailyRate: Double = EstimationDefaults.DAILY_RATE,
    val stdDevFactor: Double = EstimationDefaults.STD_DEV_FACTOR,
    val salesSurcharge: Double = EstimationDefaults.SALES_SURCHARGE,
    val effortDrivers: List<EffortDriver> = emptyList(),
    val phases: List<ProjectPhase> = emptyList(),
    val additionalCosts: List<AdditionalCost> = emptyList(),
    val roots: List<EstimationNode> = emptyList(),
    private val _id: String? = null,
    private val _createdAt: String? = null,
    private val _updatedAt: String? = null
) : BaseDomain(_id, _createdAt, _updatedAt) {

    fun calculate(): EstimationVersion {
        val totalDriverFactor = effortDrivers.sumOf { it.factor }

        val leaves = roots.flatMap { it.leaves().toList() }

        val totalVariance = leaves.sumOf { it.variance }
        val totalMean = leaves.sumOf { it.mean }
        val riskFactor = PertCalculation.riskFactor(totalMean, totalVariance, stdDevFactor)

        val params = CalculationParameters(riskFactor, totalDriverFactor, dailyRate, salesSurcharge)

        // Dispatch through the estimation method's batch hook so bucket+sampled
        // leaves get per-bucket context. A version has exactly one method, and
        // each leaf declares which one it belongs to (EstimationItem.method), so
        // core never sniffs concrete leaf types. PERT's default calculateAll is
        // per-leaf withCalculationParameters, so the PERT path stays
        // byte-identical.
        val method = leaves.firstOrNull()?.method ?: EstimationMethod.THREE_POINT_PERT
        val calculatedLeaves = EstimationMethodRegistry.require(method)
            .calculateAll(leaves, params)
            .associateBy { it.logicalId }

        val newRoots = roots.map { substituteCalculated(it, calculatedLeaves) }
        val newTotalEffort = newRoots.sumOf { it.offerPT }

        logger.debug { "calculate(): ${leaves.size} leaves, method=$method, totalEffort=$newTotalEffort" }

        return copy(roots = newRoots, totalEffort = newTotalEffort)
    }

    /**
     * The summed figures of this whole estimation.
     *
     * **Call this on the result of [calculate]** — a leaf's `cost`/`offerPrice`
     * derive from the [CalculationParameters] that [calculate] stamps onto it,
     * so on an uncalculated version the money fields are 0.
     *
     * Deliberately a MEMBER, not an extension function: `@JsExport` does not
     * export extension functions, so an extension would compile and then be
     * missing from the frontend's `domain.d.mts`.
     */
    fun totals(): EstimationTotals = computeTotals(this)

    /**
     * A rough schedule over this estimation's ROOT nodes: one scheduling unit
     * per root, `duration = offerPT / teamFte`, and the critical path through
     * the finish-to-start [dependencies] as the project length.
     *
     * **Call this on the result of [calculate]** — a node's `offerPT` derives
     * from the [CalculationParameters] that [calculate] stamps on, so on an
     * uncalculated version every duration is 0.
     *
     * Deliberately a MEMBER, not an extension function: `@JsExport` does not
     * export extension functions, so an extension would compile and then be
     * missing from the frontend's `domain.d.mts`.
     */
    fun schedule(dependencies: List<ScheduleDependency>, teamFte: Double): ProjectSchedule =
        computeSchedule(this, dependencies, teamFte)

    // Rebuild the tree with each leaf replaced by its calculated version (indexed
    // by logicalId), preserving the group structure.
    private fun substituteCalculated(
        node: EstimationNode,
        calculated: Map<String, EstimationItem>
    ): EstimationNode = when (node) {
        is EstimationGroup -> node.copy(children = node.children.map { substituteCalculated(it, calculated) })
        is EstimationItem -> calculated[node.logicalId] ?: node
    }
}
