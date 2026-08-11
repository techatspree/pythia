package io.pythia.method.bucketsampled

import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReduceBucketSampledSessionTest {

    private val delta = 1e-9
    private val t0 = Instant.parse("2026-08-08T10:00:00Z")

    private fun at(seconds: Int) = Instant.fromEpochSeconds(t0.epochSeconds + seconds)

    private fun vote(
        estimator: String,
        bucket: String,
        seconds: Int,
        sample: SampleTriple? = null
    ) = BucketVoteInput(EstimatorBucketAssignment(estimator, bucket, at(seconds)), sample)

    @Test
    fun `last write wins across estimators`() {
        val result = reduceBucketSampledSession(
            listOf(vote("alice", "S", 0), vote("bob", "L", 10), vote("carol", "M", 5))
        )

        assertEquals("L", result.assignment?.bucketId)
        assertEquals("bob", result.assignment?.source)
        assertEquals(3, result.voterCount)
    }

    @Test
    fun `an estimator's own later write supersedes their earlier one`() {
        val result = reduceBucketSampledSession(
            listOf(vote("alice", "S", 0), vote("alice", "XL", 20), vote("bob", "L", 10))
        )

        assertEquals("XL", result.assignment?.bucketId)
        assertEquals("alice", result.assignment?.source)
        // alice's superseded "S" is not a conflict — only her latest write counts.
        assertEquals(listOf("bob" to "L"), result.assignment?.conflictingAssignments?.map { it.estimatorId to it.bucketId })
    }

    @Test
    fun `losers on a different bucket are reported as conflicts`() {
        val result = reduceBucketSampledSession(
            listOf(vote("alice", "S", 0), vote("bob", "L", 10), vote("carol", "M", 5))
        )

        val conflicts = result.assignment?.conflictingAssignments.orEmpty()
        assertEquals(setOf("alice", "carol"), conflicts.map { it.estimatorId }.toSet())
        assertEquals(setOf("S", "M"), conflicts.map { it.bucketId }.toSet())
    }

    @Test
    fun `agreement on the same bucket is not a conflict`() {
        val result = reduceBucketSampledSession(
            listOf(vote("alice", "M", 0), vote("bob", "M", 10), vote("carol", "M", 5))
        )

        assertEquals("M", result.assignment?.bucketId)
        assertTrue(result.assignment?.conflictingAssignments.orEmpty().isEmpty())
    }

    @Test
    fun `equal timestamps break deterministically on estimator id`() {
        val forward = reduceBucketSampledSession(
            listOf(vote("alice", "S", 7), vote("bob", "L", 7), vote("carol", "M", 7))
        )
        // Same votes, reversed order — a bare maxByOrNull would flip the winner.
        val reversed = reduceBucketSampledSession(
            listOf(vote("carol", "M", 7), vote("bob", "L", 7), vote("alice", "S", 7))
        )

        assertEquals(forward.assignment?.bucketId, reversed.assignment?.bucketId)
        assertEquals(forward.assignment?.source, reversed.assignment?.source)
        assertEquals("carol", forward.assignment?.source)
    }

    @Test
    fun `samples average element-wise across three estimators`() {
        val result = reduceBucketSampledSession(
            listOf(
                vote("alice", "M", 0, SampleTriple(1.0, 3.0, 8.0)),
                vote("bob", "M", 1, SampleTriple(2.0, 5.0, 9.0)),
                vote("carol", "M", 2, SampleTriple(3.0, 4.0, 10.0))
            )
        )

        assertEquals(2.0, result.averagedSample!!.optimistic, delta)
        assertEquals(4.0, result.averagedSample!!.likely, delta)
        assertEquals(9.0, result.averagedSample!!.pessimistic, delta)
    }

    @Test
    fun `only the estimators who sampled contribute to the average`() {
        val result = reduceBucketSampledSession(
            listOf(
                vote("alice", "M", 0, SampleTriple(2.0, 4.0, 6.0)),
                vote("bob", "M", 1),
                vote("carol", "M", 2, SampleTriple(4.0, 6.0, 8.0))
            )
        )

        assertEquals(3.0, result.averagedSample!!.optimistic, delta)
        assertEquals(5.0, result.averagedSample!!.likely, delta)
        assertEquals(7.0, result.averagedSample!!.pessimistic, delta)
        assertEquals(3, result.voterCount)
    }

    @Test
    fun `no sample at all yields a null average, not a zero triple`() {
        val result = reduceBucketSampledSession(listOf(vote("alice", "M", 0)))

        assertNull(result.averagedSample)
        assertEquals("M", result.assignment?.bucketId)
    }

    @Test
    fun `an empty vote list reduces to an empty result`() {
        val result = reduceBucketSampledSession(emptyList())

        assertNull(result.assignment)
        assertNull(result.averagedSample)
        assertEquals(0, result.voterCount)
    }

    @Test
    fun `two buckets with distinct samples keep their own averages`() {
        // The reducer works per ITEM, so a two-bucket case is two reductions —
        // per-bucket means are applied later by EstimationVersion.calculate().
        val small = reduceBucketSampledSession(
            listOf(vote("alice", "S", 0, SampleTriple(1.0, 2.0, 3.0)), vote("bob", "S", 1, SampleTriple(3.0, 4.0, 5.0)))
        )
        val large = reduceBucketSampledSession(
            listOf(vote("alice", "L", 0, SampleTriple(10.0, 20.0, 30.0)), vote("bob", "L", 1, SampleTriple(30.0, 40.0, 50.0)))
        )

        assertEquals("S", small.assignment?.bucketId)
        assertEquals(3.0, small.averagedSample!!.likely, delta)
        assertEquals("L", large.assignment?.bucketId)
        assertEquals(30.0, large.averagedSample!!.likely, delta)
    }
}
