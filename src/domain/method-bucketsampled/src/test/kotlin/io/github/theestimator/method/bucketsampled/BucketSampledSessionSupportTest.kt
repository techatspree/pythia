package io.github.theestimator.method.bucketsampled

import io.github.theestimator.method.EstimationMethod
import io.github.theestimator.method.EstimationMethodRegistry
import io.github.theestimator.method.SessionVoteInput
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// This is a METHOD module, so it cannot see the aggregator's StandardMethods —
// it registers its own support directly. Registering only adds/overwrites, so
// unlike clear() it cannot race a sibling class in this module's test JVM.
class BucketSampledSessionSupportTest {

    private val support = BucketSampledSessionSupport()
    private val at = Instant.parse("2026-08-08T10:00:00Z")

    /** Stands in for another method's vote shape (PERT's PertVoteInput). */
    private object ForeignVoteInput : SessionVoteInput

    private fun vote(estimator: String, bucket: String) =
        BucketVoteInput(EstimatorBucketAssignment(estimator, bucket, at))

    @BeforeEach
    fun registerOwnSupport() {
        EstimationMethodRegistry.registerSessionSupport(BucketSampledSessionSupport())
    }

    @Test
    fun `reduce reports the bucket-sampled method`() {
        assertEquals(EstimationMethod.BUCKET_SAMPLED_PERT, support.method)
    }

    @Test
    fun `reduce is identical to the pure reducer for the same input`() {
        val votes = listOf(vote("alice", "S"), vote("bob", "L"))

        assertEquals(reduceBucketSampledSession(votes), support.reduce(votes))
    }

    @Test
    fun `reduce rejects a foreign vote input instead of filtering it away`() {
        // Silently dropping it would reduce the remainder and return a
        // plausible but wrong group result.
        val mixed = listOf(vote("alice", "S"), ForeignVoteInput)

        assertThrows(IllegalStateException::class.java) { support.reduce(mixed) }
        assertThrows(IllegalStateException::class.java) { support.reduce(listOf(ForeignVoteInput)) }
    }

    @Test
    fun `the support is resolvable from the registry once registered`() {
        assertNotNull(EstimationMethodRegistry.getSession(EstimationMethod.BUCKET_SAMPLED_PERT))
        assertEquals(
            EstimationMethod.BUCKET_SAMPLED_PERT,
            EstimationMethodRegistry.requireSession(EstimationMethod.BUCKET_SAMPLED_PERT).method
        )
    }

    @Test
    fun `requireSession returns the registered instance`() {
        val registered = BucketSampledSessionSupport()
        EstimationMethodRegistry.registerSessionSupport(registered)

        assertSame(registered, EstimationMethodRegistry.requireSession(EstimationMethod.BUCKET_SAMPLED_PERT))
    }
}
