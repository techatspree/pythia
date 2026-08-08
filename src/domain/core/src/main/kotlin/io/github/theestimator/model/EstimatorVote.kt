@file:OptIn(ExperimentalJsExport::class)

package io.github.theestimator.model

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/**
 * One estimator's three-point PERT vote for a single work item in a
 * collaborative estimation session. A value object (no identity), so it does
 * not extend BaseDomain.
 *
 * This is the THREE_POINT_PERT vote shape specifically — the bucket+sampled
 * method votes differently (a per-item bucket assignment, task-106). See
 * task-062: `VoteAggregation` reduces a list of these into a group estimate.
 */
@JsExport
data class EstimatorVote(
    val minEffort: Double,
    val expectedEffort: Double,
    val maxEffort: Double
)
