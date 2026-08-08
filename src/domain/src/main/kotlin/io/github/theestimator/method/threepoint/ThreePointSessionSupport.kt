package io.github.theestimator.method.threepoint

import io.github.theestimator.method.EstimationMethod
import io.github.theestimator.method.EstimationMethodSessionSupport
import io.github.theestimator.model.EstimatorVote
import io.github.theestimator.model.VoteAggregate
import io.github.theestimator.model.VoteAggregation
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Session support for three-point PERT (method #1) behind the session SPI.
 *
 * The reduction itself stays in [VoteAggregation] — the frontend's
 * `aggregateVotes` bridge and `VoteAggregationTest` reference it directly, so
 * this class is a dispatch seam, not a new home for the math.
 */
class ThreePointSessionSupport : EstimationMethodSessionSupport {
    override val method: EstimationMethod = EstimationMethod.THREE_POINT_PERT

    override fun reduce(votes: List<EstimatorVote>): VoteAggregate {
        logger.debug { "Reducing ${votes.size} vote(s) for $method" }
        return VoteAggregation.aggregate(votes)
    }
}
