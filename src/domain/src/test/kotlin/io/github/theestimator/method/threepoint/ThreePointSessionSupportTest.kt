package io.github.theestimator.method.threepoint

import io.github.theestimator.method.EstimationMethod
import io.github.theestimator.method.EstimationMethodRegistry
import io.github.theestimator.model.EstimatorVote
import io.github.theestimator.model.VoteAggregation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

// NOTE: this class deliberately never calls EstimationMethodRegistry.clear().
// The registry is a process-wide singleton and the test class order is
// non-deterministic, so clearing it here could empty it mid-read for a
// concurrently running class (see the comment in BucketMethodModuleTest).
// installStandardMethods() is idempotent, so re-installing is safe.
class ThreePointSessionSupportTest {

    private val support = ThreePointSessionSupport()
    private val delta = 0.000001

    private val votes = listOf(
        EstimatorVote(1.0, 3.0, 8.0),
        EstimatorVote(2.0, 5.0, 9.0),
        EstimatorVote(3.0, 4.0, 10.0)
    )

    @Test
    fun `reduce reports the PERT method`() {
        assertEquals(EstimationMethod.THREE_POINT_PERT, support.method)
    }

    @Test
    fun `reduce is identical to the domain VoteAggregation for the same input`() {
        val expected = VoteAggregation.aggregate(votes)
        val actual = support.reduce(votes)

        assertEquals(expected.meanMin, actual.meanMin, delta)
        assertEquals(expected.meanExpected, actual.meanExpected, delta)
        assertEquals(expected.meanMax, actual.meanMax, delta)
        assertEquals(expected.pertMean, actual.pertMean, delta)
        assertEquals(expected.expectedStdDev, actual.expectedStdDev, delta)
        assertEquals(expected.expectedCv, actual.expectedCv, delta)
        assertEquals(expected.diverged, actual.diverged)
        assertEquals(expected.voterCount, actual.voterCount)
        // Data class equality — the seam must not reshape the result at all.
        assertEquals(expected, actual)
    }

    @Test
    fun `reduce handles the empty vote list like the domain reducer`() {
        assertEquals(VoteAggregation.aggregate(emptyList()), support.reduce(emptyList()))
    }

    @Test
    fun `installStandardMethods registers the PERT session support`() {
        EstimationMethodRegistry.installStandardMethods()

        assertNotNull(EstimationMethodRegistry.getSession(EstimationMethod.THREE_POINT_PERT))
        assertEquals(
            EstimationMethod.THREE_POINT_PERT,
            EstimationMethodRegistry.requireSession(EstimationMethod.THREE_POINT_PERT).method
        )
    }

    @Test
    fun `requireSession returns the registered instance`() {
        val registered = ThreePointSessionSupport()
        EstimationMethodRegistry.registerSessionSupport(registered)

        assertSame(registered, EstimationMethodRegistry.requireSession(EstimationMethod.THREE_POINT_PERT))

        // Restore the standard instance for any later test class.
        EstimationMethodRegistry.installStandardMethods()
    }
}
