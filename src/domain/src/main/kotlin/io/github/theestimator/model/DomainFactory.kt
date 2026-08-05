@file:OptIn(ExperimentalJsExport::class)
// These are @JsExport factory functions consumed by the frontend (adapter.ts).
// Their parameter lists mirror the domain entities' fields; collapsing them into
// a params object would change the public Kotlin/JS API the frontend binds to.
@file:Suppress("LongParameterList")

package io.github.theestimator.model

import io.github.theestimator.method.bucketsampled.BucketedEstimationItem
import io.github.theestimator.method.threepoint.FixedEstimationItem
import io.github.theestimator.method.threepoint.TimeRelativeEstimationItem
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
    roots: Array<EstimationNode> = emptyArray()
): EstimationVersion = EstimationVersion(
    versionNumber = versionNumber,
    status = if (isDraft) EstimationVersionStatus.DRAFT else EstimationVersionStatus.SUBMITTED,
    notes = notes,
    dailyRate = dailyRate,
    stdDevFactor = stdDevFactor,
    salesSurcharge = salesSurcharge,
    effortDrivers = effortDrivers.toList(),
    phases = phases.toList(),
    roots = roots.toList()
)
