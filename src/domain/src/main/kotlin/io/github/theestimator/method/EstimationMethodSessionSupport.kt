package io.github.theestimator.method

import io.github.theestimator.model.EstimatorVote
import io.github.theestimator.model.VoteAggregate

/**
 * Per-method behaviour for collaborative estimation sessions — a sibling SPI to
 * [EstimationMethodModule], not a nested part of it: session support is
 * inherently multi-estimator, while the module covers per-leaf calculation and
 * export shaping. Keeping them separate avoids one over-general interface.
 *
 * The vote types are the ones task-062 already shipped
 * ([EstimatorVote] / [VoteAggregate]) rather than method-neutral abstractions —
 * they are `@JsExport`ed and load-bearing in the wire DTOs and the frontend, so
 * a parallel type hierarchy would only duplicate them. When the bucket+sampled
 * method introduces its own vote shape (task-106) this signature generalises
 * against a real second implementation.
 *
 * Domain-internal like [EstimationMethodModule] and [EstimationMethodRegistry]:
 * only the [EstimationMethod] enum is `@JsExport`ed.
 */
interface EstimationMethodSessionSupport {
    val method: EstimationMethod

    /** Reduce the estimators' votes for one work item into the group estimate. */
    fun reduce(votes: List<EstimatorVote>): VoteAggregate
}
