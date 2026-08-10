package io.github.theestimator.method.bucketsampled

import io.github.theestimator.method.SessionReduction
import io.github.theestimator.method.SessionVoteInput
import kotlinx.datetime.Instant

/**
 * One estimator's bucket choice for the work item under discussion.
 *
 * [at] is `kotlinx.datetime.Instant`, NOT `java.time.Instant`: this is
 * `commonMain`, so a JVM-only type would compile and pass `jvmTest` and then
 * break the Kotlin/JS compilation. The backend maps its `java.time.Instant` at
 * the service boundary.
 */
data class EstimatorBucketAssignment(
    val estimatorId: String,
    val bucketId: String,
    val at: Instant
)

/** One estimator's optional three-point sample for the item. */
data class SampleTriple(
    val optimistic: Double,
    val likely: Double,
    val pessimistic: Double
)

/**
 * The bucket the group landed on, who wrote it, and every assignment that lost
 * the last-write-wins race. [conflictingAssignments] is what the session UI
 * surfaces so a disagreement is visible rather than silently resolved.
 */
data class AssignmentResult(
    val bucketId: String,
    val source: String,
    val conflictingAssignments: List<EstimatorBucketAssignment>
)

/** One estimator's vote on the current item: a bucket, plus a sample if they gave one. */
data class BucketVoteInput(
    val assignment: EstimatorBucketAssignment,
    val sample: SampleTriple? = null
) : SessionVoteInput

/**
 * The reduced group estimate for ONE work item: the LWW-resolved bucket with
 * its provenance, and the element-wise average of whatever samples were cast.
 *
 * Deliberately NOT per-bucket means. The session decides each item's bucket and
 * sample triple; those are written back onto the draft leaf, and the per-bucket
 * averaging is then applied by `EstimationVersion.calculate()` through
 * `BucketMethodModule.calculateAll` / `computeBucketAverages`. Computing it here
 * as well would duplicate that reducer and break single-source-of-truth.
 */
data class BucketSampledSessionResult(
    val assignment: AssignmentResult?,
    val averagedSample: SampleTriple?,
    val voterCount: Int
) : SessionReduction
