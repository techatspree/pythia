package io.github.theestimator.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VoteAggregationTest {

    private val delta = 1e-9

    @Test
    fun `empty list yields a zeroed aggregate`() {
        val agg = VoteAggregation.aggregate(emptyList())

        assertEquals(0, agg.voterCount)
        assertFalse(agg.diverged)
        assertEquals(0.0, agg.meanMin, delta)
        assertEquals(0.0, agg.meanExpected, delta)
        assertEquals(0.0, agg.meanMax, delta)
        assertEquals(0.0, agg.pertMean, delta)
        assertEquals(0.0, agg.expectedStdDev, delta)
        assertEquals(0.0, agg.expectedCv, delta)
        assertEquals(0.0, agg.expectedRange, delta)
    }

    @Test
    fun `single vote is returned as-is with zero spread`() {
        val agg = VoteAggregation.aggregate(listOf(EstimatorVote(2.0, 4.0, 6.0)))

        assertEquals(1, agg.voterCount)
        assertEquals(2.0, agg.meanMin, delta)
        assertEquals(4.0, agg.meanExpected, delta)
        assertEquals(6.0, agg.meanMax, delta)
        // PERT mean of (2, 4, 6) = (2 + 4*4 + 6) / 6 = 4.0
        assertEquals(4.0, agg.pertMean, delta)
        assertEquals(0.0, agg.expectedStdDev, delta)
        assertEquals(0.0, agg.expectedCv, delta)
        assertFalse(agg.diverged)
    }

    @Test
    fun `field-wise means are the per-field averages`() {
        val agg = VoteAggregation.aggregate(
            listOf(
                EstimatorVote(1.0, 2.0, 3.0),
                EstimatorVote(3.0, 4.0, 5.0),
                EstimatorVote(5.0, 6.0, 7.0)
            )
        )

        assertEquals(3, agg.voterCount)
        assertEquals(3.0, agg.meanMin, delta)
        assertEquals(4.0, agg.meanExpected, delta)
        assertEquals(5.0, agg.meanMax, delta)
        // PERT mean of the averaged triple (3, 4, 5) = (3 + 16 + 5) / 6 = 4.0
        assertEquals(4.0, agg.pertMean, delta)
        assertEquals(2.0, agg.expectedMin, delta)
        assertEquals(6.0, agg.expectedMax, delta)
        assertEquals(4.0, agg.expectedRange, delta)
    }

    @Test
    fun `several close votes do not diverge`() {
        val agg = VoteAggregation.aggregate(
            listOf(
                EstimatorVote(3.0, 4.0, 5.0),
                EstimatorVote(3.0, 4.2, 5.0),
                EstimatorVote(3.0, 3.8, 5.0)
            )
        )

        // expected = [4.0, 4.2, 3.8], mean 4.0, sample stdDev 0.2 → CV 0.05
        assertEquals(4.0, agg.meanExpected, delta)
        assertEquals(0.2, agg.expectedStdDev, delta)
        assertEquals(0.05, agg.expectedCv, delta)
        assertFalse(agg.diverged)
    }

    @Test
    fun `a clear outlier pushes the coefficient of variation above the threshold`() {
        val agg = VoteAggregation.aggregate(
            listOf(
                EstimatorVote(1.0, 2.0, 3.0),
                EstimatorVote(2.0, 4.0, 6.0),
                EstimatorVote(6.0, 12.0, 18.0)
            )
        )

        // expected = [2, 4, 12], mean 6, sample variance 28 → stdDev sqrt(28) ≈ 5.29,
        // CV ≈ 0.88 > 0.20
        assertEquals(6.0, agg.meanExpected, delta)
        assertTrue(agg.expectedCv > VoteAggregation.DIVERGENCE_CV_THRESHOLD)
        assertTrue(agg.diverged)
    }

    @Test
    fun `zero expected mean yields CV 0 without dividing by zero`() {
        val agg = VoteAggregation.aggregate(
            listOf(
                EstimatorVote(0.0, 0.0, 0.0),
                EstimatorVote(0.0, 0.0, 0.0)
            )
        )

        assertEquals(0.0, agg.meanExpected, delta)
        assertEquals(0.0, agg.expectedCv, delta)
        assertFalse(agg.diverged)
    }
}
