package io.github.theestimator.method.threepoint

import io.github.theestimator.method.SessionReduction
import io.github.theestimator.method.SessionVoteInput
import io.github.theestimator.model.EstimatorVote
import io.github.theestimator.model.VoteAggregate

/**
 * PERT's session vote: one estimator's three-point triple for a work item.
 *
 * [EstimatorVote] is WRAPPED rather than made to implement [SessionVoteInput]
 * directly. It lives in `io.github.theestimator.model`, is `@JsExport`ed, and is
 * consumed by the frontend through `DomainFactory.aggregateVotes`; giving it a
 * domain-internal supertype would change the generated `domain.d.mts` surface
 * for no benefit.
 */
data class PertVoteInput(val vote: EstimatorVote) : SessionVoteInput

/**
 * PERT's session reduction: the averaged triple plus its spread, exactly as
 * `VoteAggregation` computes it. [VoteAggregate] is wrapped for the same reason
 * [PertVoteInput] wraps [EstimatorVote].
 */
data class PertReduction(val aggregate: VoteAggregate) : SessionReduction
