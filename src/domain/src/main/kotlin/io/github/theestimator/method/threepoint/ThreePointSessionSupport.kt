package io.github.theestimator.method.threepoint

import io.github.theestimator.method.EstimationMethod
import io.github.theestimator.method.EstimationMethodSessionSupport
import io.github.theestimator.method.SessionReduction
import io.github.theestimator.method.SessionVoteInput
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

    override fun reduce(votes: List<SessionVoteInput>): SessionReduction {
        logger.debug { "Reducing ${votes.size} vote(s) for $method" }
        val triples = votes.map {
            // Fail loudly on a foreign input rather than filtering it away: a
            // mismatched vote shape means the session was dispatched to the
            // wrong method, and silently averaging the remainder would produce
            // a plausible but wrong group estimate.
            (it as? PertVoteInput ?: error("$method cannot reduce ${it::class.simpleName}")).vote
        }
        return PertReduction(VoteAggregation.aggregate(triples))
    }
}
