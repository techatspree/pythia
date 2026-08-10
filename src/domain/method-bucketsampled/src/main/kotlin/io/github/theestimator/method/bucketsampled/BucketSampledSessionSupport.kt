package io.github.theestimator.method.bucketsampled

import io.github.theestimator.method.EstimationMethod
import io.github.theestimator.method.EstimationMethodSessionSupport
import io.github.theestimator.method.SessionReduction
import io.github.theestimator.method.SessionVoteInput
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Session support for bucket + sampled PERT (method #2) behind the session SPI.
 *
 * A dispatch seam like `ThreePointSessionSupport`: the reduction itself lives in
 * [reduceBucketSampledSession].
 */
class BucketSampledSessionSupport : EstimationMethodSessionSupport {
    override val method: EstimationMethod = EstimationMethod.BUCKET_SAMPLED_PERT

    override fun reduce(votes: List<SessionVoteInput>): SessionReduction {
        logger.debug { "Reducing ${votes.size} vote(s) for $method" }
        val bucketVotes = votes.map {
            // Fail loudly on a foreign input rather than filtering it away: a
            // mismatched vote shape means the session was dispatched to the
            // wrong method, and quietly reducing the remainder would yield a
            // plausible but wrong group result.
            it as? BucketVoteInput ?: error("$method cannot reduce ${it::class.simpleName}")
        }
        return reduceBucketSampledSession(bucketVotes)
    }
}
