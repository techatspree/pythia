@file:OptIn(ExperimentalJsExport::class)

package io.github.theestimator.model

import io.github.theestimator.method.EstimationMethod
import io.github.theestimator.method.EstimationMethodRegistry
import io.github.theestimator.method.bucketsampled.BucketedEstimationItem
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
        // leaves get per-bucket context. A version has exactly one method, so the
        // method is detected from the leaf types; PERT's default calculateAll is
        // per-leaf withCalculationParameters, so the PERT path is byte-identical.
        val method = if (leaves.any { it is BucketedEstimationItem }) {
            EstimationMethod.BUCKET_SAMPLED_PERT
        } else {
            EstimationMethod.THREE_POINT_PERT
        }
        val calculatedLeaves = EstimationMethodRegistry.require(method)
            .calculateAll(leaves, params)
            .associateBy { it.logicalId }

        val newRoots = roots.map { substituteCalculated(it, calculatedLeaves) }
        val newTotalEffort = newRoots.sumOf { it.offerPT }

        logger.debug { "calculate(): ${leaves.size} leaves, method=$method, totalEffort=$newTotalEffort" }

        return copy(roots = newRoots, totalEffort = newTotalEffort)
    }

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
