@file:OptIn(ExperimentalJsExport::class)

package io.github.theestimator.model

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.math.sqrt

private val logger = KotlinLogging.logger {}

/**
 * The reduced group estimate for one work item from a set of three-point PERT
 * votes: the field-wise averaged triple plus its PERT mean, and the spread of
 * the convergence-driving field (`expectedEffort`) so the moderator can see
 * when estimates differ too strongly.
 */
@JsExport
data class VoteAggregate(
    val meanMin: Double,
    val meanExpected: Double,
    val meanMax: Double,
    val pertMean: Double,
    val expectedMin: Double,
    val expectedMax: Double,
    val expectedRange: Double,
    val expectedStdDev: Double,
    val expectedCv: Double,
    val diverged: Boolean,
    val voterCount: Int
)

/**
 * Pure THREE_POINT_PERT session reduction: averages estimators' triples
 * field by field and measures their spread. The single source of truth for
 * both the backend finalize write-back and the live phase-2 display.
 */
@JsExport
object VoteAggregation {

    /**
     * Estimates whose `expectedEffort` coefficient of variation exceeds this
     * threshold are flagged as diverged for the moderator (20% CV).
     */
    const val DIVERGENCE_CV_THRESHOLD = 0.20

    fun aggregate(votes: List<EstimatorVote>): VoteAggregate {
        val n = votes.size
        if (n == 0) {
            return VoteAggregate(
                meanMin = 0.0,
                meanExpected = 0.0,
                meanMax = 0.0,
                pertMean = 0.0,
                expectedMin = 0.0,
                expectedMax = 0.0,
                expectedRange = 0.0,
                expectedStdDev = 0.0,
                expectedCv = 0.0,
                diverged = false,
                voterCount = 0
            )
        }

        val meanMin = votes.sumOf { it.minEffort } / n
        val meanExpected = votes.sumOf { it.expectedEffort } / n
        val meanMax = votes.sumOf { it.maxEffort } / n
        val pertMean = PertCalculation.mean(meanMin, meanExpected, meanMax)

        val expected = votes.map { it.expectedEffort }
        val expectedMin = expected.min()
        val expectedMax = expected.max()
        val expectedStdDev = sampleStdDev(expected, meanExpected)
        val expectedCv = if (meanExpected == 0.0) 0.0 else expectedStdDev / meanExpected
        val diverged = expectedCv > DIVERGENCE_CV_THRESHOLD

        logger.debug { "aggregated $n votes: cv=$expectedCv diverged=$diverged" }

        return VoteAggregate(
            meanMin = meanMin,
            meanExpected = meanExpected,
            meanMax = meanMax,
            pertMean = pertMean,
            expectedMin = expectedMin,
            expectedMax = expectedMax,
            expectedRange = expectedMax - expectedMin,
            expectedStdDev = expectedStdDev,
            expectedCv = expectedCv,
            diverged = diverged,
            voterCount = n
        )
    }

    /** Sample standard deviation (n-1 denominator); 0.0 for a single value. */
    private fun sampleStdDev(values: List<Double>, mean: Double): Double {
        val n = values.size
        if (n <= 1) return 0.0
        val sumSquaredDeviations = values.sumOf { val d = it - mean; d * d }
        return sqrt(sumSquaredDeviations / (n - 1))
    }
}
