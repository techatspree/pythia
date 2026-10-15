package io.pythia.method

import io.pythia.StandardMethods
import io.pythia.method.bucketsampled.BucketedEstimationItem
import io.pythia.method.threepoint.TimeRelativeEstimationItem
import io.pythia.model.EstimationVersion
import io.pythia.model.ProjectPhase
import io.pythia.model.leaves
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Accompanying work inside a bucket + sampled estimation (task-187). It lives in
 * the aggregator because it needs both methods' leaf types, which the method
 * modules cannot see of each other (task-143).
 *
 * `TimeRelativeEstimationItem.method` says THREE_POINT_PERT; this pins that it
 * is still calculated inside a BUCKET_SAMPLED_PERT version, by the bucket
 * module's `calculateAll`, without disturbing the bucket averages.
 */
class BucketVersionWithTimeRelativeTest {

    private val delta = 1e-9

    @BeforeEach
    fun ensureRegistryPopulated() {
        StandardMethods.installAll()
    }

    @Test
    fun `accompanying work is calculated inside a bucket version`() {
        val umsetzung = ProjectPhase(name = "Umsetzung", abbreviation = "UM", durationWeeks = 12.0)
        val version = EstimationVersion(
            versionNumber = 1,
            stdDevFactor = 2.0,
            dailyRate = 800.0,
            salesSurcharge = 0.1,
            roots = listOf(
                BucketedEstimationItem(
                    bucketId = "b1", isSample = true, optimistic = 1.0, likely = 2.0, pessimistic = 3.0
                ),
                BucketedEstimationItem(bucketId = "b1", isSample = false),
                TimeRelativeEstimationItem(
                    _description = "Projektbegleitung",
                    _minEffort = 0.5,
                    _expectedEffort = 1.0,
                    _maxEffort = 2.0,
                    _phase = umsetzung
                )
            )
        ).calculate()

        val leaves = version.roots.flatMap { it.leaves().toList() }
        val accompanying = leaves.filterIsInstance<TimeRelativeEstimationItem>().single()
        // PERT(0.5, 1.0, 2.0) = 1.0833… per week, × 12 weeks.
        assertEquals(13.0, accompanying.mean, delta)

        // The non-sample still inherits its bucket's sample mean.
        val nonSample = leaves.filterIsInstance<BucketedEstimationItem>().single { !it.isSample }
        assertEquals(2.0, nonSample.mean, delta)

        // And the version total is the sum of all three leaves.
        assertEquals(leaves.sumOf { it.mean }, version.roots.sumOf { it.mean }, delta)
        assertEquals(17.0, version.roots.sumOf { it.mean }, delta)
    }
}
