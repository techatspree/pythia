@file:OptIn(ExperimentalJsExport::class)
// These are @JsExport factory functions consumed by the frontend (adapter.ts).
// Their parameter lists mirror the domain entities' fields; collapsing them into
// a params object would change the public Kotlin/JS API the frontend binds to.
@file:Suppress("LongParameterList")

// Lives in the AGGREGATOR :domain, not in :domain:core (task-143): these
// factories construct concrete leaves from every method module, which core is
// forbidden to see. The package is `io.github.theestimator` rather than
// `…estimation.model` so that package is not split across two Gradle modules —
// which would hide core's `internal` declarations from this file. The rename is
// invisible to the frontend: the generated domain.d.mts exports everything flat
// at top level, not nested by Kotlin package.
package io.github.theestimator

import io.github.theestimator.method.bucketsampled.BucketedEstimationItem
import io.github.theestimator.method.threepoint.FixedEstimationItem
import io.github.theestimator.method.threepoint.TimeRelativeEstimationItem
import io.github.theestimator.model.AdditionalCost
import io.github.theestimator.model.EffortDriver
import io.github.theestimator.model.EstimationDefaults
import io.github.theestimator.model.EstimationGroup
import io.github.theestimator.model.EstimationNode
import io.github.theestimator.model.EstimationVersion
import io.github.theestimator.model.EstimationVersionStatus
import io.github.theestimator.model.EstimatorVote
import io.github.theestimator.model.ProjectPhase
import io.github.theestimator.model.VoteAggregate
import io.github.theestimator.model.VoteAggregation
import io.github.theestimator.model.newId
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@JsExport
fun createFixedItem(
    description: String,
    minEffort: Double = 0.0,
    expectedEffort: Double = 0.0,
    maxEffort: Double = 0.0,
    assumptions: String = "",
    logicalId: String = newId()
): FixedEstimationItem = FixedEstimationItem(
    _description = description,
    _minEffort = minEffort,
    _expectedEffort = expectedEffort,
    _maxEffort = maxEffort,
    _assumptions = assumptions,
    _logicalId = logicalId
)

@JsExport
fun createTimeRelativeItem(
    description: String,
    unit: String = "h/Woche",
    minEffort: Double = 0.0,
    expectedEffort: Double = 0.0,
    maxEffort: Double = 0.0,
    assumptions: String = "",
    logicalId: String = newId(),
    phase: ProjectPhase? = null
): TimeRelativeEstimationItem = TimeRelativeEstimationItem(
    unit = unit,
    _description = description,
    _minEffort = minEffort,
    _expectedEffort = expectedEffort,
    _maxEffort = maxEffort,
    _assumptions = assumptions,
    _logicalId = logicalId,
    _phase = phase
)

@JsExport
fun createBucketedItem(
    description: String,
    bucketId: String,
    isSample: Boolean = false,
    optimistic: Double = 0.0,
    likely: Double = 0.0,
    pessimistic: Double = 0.0,
    logicalId: String = newId()
): BucketedEstimationItem = BucketedEstimationItem(
    bucketId = bucketId,
    isSample = isSample,
    optimistic = optimistic,
    likely = likely,
    pessimistic = pessimistic,
    _description = description,
    _logicalId = logicalId
)

@JsExport
fun createGroup(
    title: String,
    logicalId: String = newId(),
    children: Array<EstimationNode> = emptyArray()
): EstimationGroup = EstimationGroup(
    title = title,
    children = children.toList(),
    _logicalId = logicalId
)

// JS-friendly wrapper over VoteAggregation (task-062) so the frontend session
// UI (task-067) can reduce a set of estimator votes with the SAME domain code the
// backend uses — VoteAggregation stays the single source of truth; this only
// adapts a JS array to the Kotlin List the reducer expects.
@JsExport
fun aggregateVotes(votes: Array<EstimatorVote>): VoteAggregate =
    VoteAggregation.aggregate(votes.toList())

/**
 * Bootstrap for non-JVM callers. The registry no longer self-populates
 * (task-143), and the frontend has no startup hook of its own, so the one
 * factory whose result gets `calculate()`d — [createVersion] — installs the
 * standard methods first. Idempotent and cheap.
 */
private fun ensureMethodsInstalled() = StandardMethods.installAll()

@JsExport
fun createVersion(
    versionNumber: Int,
    isDraft: Boolean,
    notes: String = "",
    dailyRate: Double = EstimationDefaults.DAILY_RATE,
    stdDevFactor: Double = EstimationDefaults.STD_DEV_FACTOR,
    salesSurcharge: Double = EstimationDefaults.SALES_SURCHARGE,
    effortDrivers: Array<EffortDriver> = emptyArray(),
    phases: Array<ProjectPhase> = emptyArray(),
    additionalCosts: Array<AdditionalCost> = emptyArray(),
    roots: Array<EstimationNode> = emptyArray()
): EstimationVersion {
    ensureMethodsInstalled()
    return EstimationVersion(
        versionNumber = versionNumber,
        status = if (isDraft) EstimationVersionStatus.DRAFT else EstimationVersionStatus.SUBMITTED,
        notes = notes,
        dailyRate = dailyRate,
        stdDevFactor = stdDevFactor,
        salesSurcharge = salesSurcharge,
        effortDrivers = effortDrivers.toList(),
        phases = phases.toList(),
        additionalCosts = additionalCosts.toList(),
        roots = roots.toList()
    )
}
