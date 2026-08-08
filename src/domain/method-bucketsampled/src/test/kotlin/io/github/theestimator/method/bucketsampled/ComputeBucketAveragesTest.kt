package io.github.theestimator.method.bucketsampled

import io.github.theestimator.model.CalculationParameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ComputeBucketAveragesTest {

    private fun sample(bucket: String, o: Double, l: Double, p: Double) =
        BucketedEstimationItem(bucketId = bucket, isSample = true, optimistic = o, likely = l, pessimistic = p)

    @Test
    fun `three samples in one bucket average their PERT means`() {
        val samples = listOf(
            sample("b1", 1.0, 2.0, 3.0),   // mean 2.0
            sample("b1", 2.0, 4.0, 6.0),   // mean 4.0
            sample("b1", 3.0, 6.0, 9.0)    // mean 6.0
        )
        val averages = computeBucketAverages(samples, CalculationParameters())
        assertEquals(1, averages.size)
        assertEquals((2.0 + 4.0 + 6.0) / 3.0, averages.getValue("b1").mean, 1e-9)
        // default params → offerPT == mean
        assertEquals((2.0 + 4.0 + 6.0) / 3.0, averages.getValue("b1").offerPT, 1e-9)
    }

    @Test
    fun `two buckets in one call are averaged independently`() {
        val samples = listOf(
            sample("b1", 1.0, 2.0, 3.0),   // mean 2.0
            sample("b1", 3.0, 6.0, 9.0),   // mean 6.0
            sample("b2", 10.0, 10.0, 10.0) // mean 10.0
        )
        val averages = computeBucketAverages(samples, CalculationParameters())
        assertEquals(setOf("b1", "b2"), averages.keys)
        assertEquals(4.0, averages.getValue("b1").mean, 1e-9)
        assertEquals(10.0, averages.getValue("b2").mean, 1e-9)
    }

    @Test
    fun `a bucket with only non-sample items produces no entry`() {
        val items = listOf(
            BucketedEstimationItem(bucketId = "b1", isSample = false),
            BucketedEstimationItem(bucketId = "b1", isSample = false)
        )
        val averages = computeBucketAverages(items, CalculationParameters())
        assertTrue(averages.isEmpty())
        assertFalse(averages.containsKey("b1"))
    }
}
