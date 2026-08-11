package io.pythia.method.bucketsampled

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Pure BUCKET_SAMPLED_PERT session reduction for ONE work item: resolve the
 * bucket by last-write-wins (keeping the losers as provenance) and average
 * whatever three-point samples the estimators cast.
 *
 * The single source of truth for both the backend's finalize write-back and the
 * live phase-2 display, mirroring `VoteAggregation` on the PERT side.
 */
fun reduceBucketSampledSession(votes: List<BucketVoteInput>): BucketSampledSessionResult {
    val result = BucketSampledSessionResult(
        assignment = resolveAssignmentLww(votes.map { it.assignment }),
        averagedSample = averageSamplePert(votes.mapNotNull { it.sample }),
        voterCount = votes.size
    )
    logger.debug {
        "reduced ${votes.size} bucket vote(s): bucket=${result.assignment?.bucketId} " +
            "conflicts=${result.assignment?.conflictingAssignments?.size ?: 0} " +
            "sampled=${result.averagedSample != null}"
    }
    return result
}

/**
 * Last-write-wins: keep each estimator's most recent write, then take the most
 * recent of those. Everyone else's latest write becomes a conflict entry.
 *
 * Ties on [EstimatorBucketAssignment.at] break on `estimatorId` so the reducer
 * stays a pure function — two votes can share a timestamp (same clock tick, or
 * a fixture), and a bare `maxByOrNull { it.at }` would then depend on list
 * order and produce flaky results.
 */
private fun resolveAssignmentLww(assignments: List<EstimatorBucketAssignment>): AssignmentResult? {
    if (assignments.isEmpty()) return null
    val order = compareBy<EstimatorBucketAssignment>({ it.at }, { it.estimatorId })

    val latestPerEstimator = assignments
        .groupBy { it.estimatorId }
        .map { (_, byEstimator) -> byEstimator.maxWith(order) }

    val winner = latestPerEstimator.maxWith(order)
    return AssignmentResult(
        bucketId = winner.bucketId,
        source = winner.estimatorId,
        // Only genuine disagreement is a conflict: an estimator who picked the
        // same bucket agreed, they did not lose a race.
        conflictingAssignments = latestPerEstimator
            .filter { it.estimatorId != winner.estimatorId && it.bucketId != winner.bucketId }
            .sortedWith(order)
    )
}

/** Element-wise mean of the cast samples; `null` when nobody sampled this item. */
private fun averageSamplePert(samples: List<SampleTriple>): SampleTriple? {
    if (samples.isEmpty()) return null
    return SampleTriple(
        optimistic = samples.map { it.optimistic }.average(),
        likely = samples.map { it.likely }.average(),
        pessimistic = samples.map { it.pessimistic }.average()
    )
}
