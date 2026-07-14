package io.github.theestimator.method.bucketsampled

import io.github.theestimator.model.CalculationParameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class BucketedEstimationItemTest {

    @Test
    fun `sample row derives PERT mean and variance from three-point values`() {
        val item = BucketedEstimationItem(
            bucketId = "b1",
            isSample = true,
            optimistic = 1.0,
            likely = 2.0,
            pessimistic = 3.0
        )
        assertEquals(2.0, item.mean, 1e-9)                       // (1 + 4*2 + 3) / 6
        assertEquals((2.0 / 6.0) * (2.0 / 6.0), item.variance, 1e-9)  // ((3-1)/6)^2
    }

    @Test
    fun `sample offerPT applies risk and driver surcharge`() {
        val item = BucketedEstimationItem(
            bucketId = "b1", isSample = true,
            optimistic = 1.0, likely = 2.0, pessimistic = 3.0
        ).withCalculationParameters(CalculationParameters(riskFactor = 0.1, totalDriverFactor = 0.2))
        assertEquals(2.0 * (1 + 0.1 + 0.2), item.offerPT, 1e-9)
    }

    @Test
    fun `non-sample row returns zero neutral values until a bucket average is assigned`() {
        val item = BucketedEstimationItem(bucketId = "b1", isSample = false)
        assertFalse(item.isSample)
        assertEquals(0.0, item.mean, 1e-9)
        assertEquals(0.0, item.variance, 1e-9)
        assertEquals(0.0, item.offerPT, 1e-9)
    }

    @Test
    fun `withCalculatedFromBucket copies the bucket-averaged neutral values`() {
        val assigned = BucketedEstimationItem(bucketId = "b1", isSample = false)
            .withCalculatedFromBucket(mean = 5.0, variance = 1.0, offerPT = 6.0)
        assertEquals(5.0, assigned.mean, 1e-9)
        assertEquals(1.0, assigned.variance, 1e-9)
        assertEquals(6.0, assigned.offerPT, 1e-9)
    }

    @Test
    fun `cost and offerPrice on a non-sample row derive from the assigned offerPT`() {
        val assigned = BucketedEstimationItem(bucketId = "b1", isSample = false)
            .withCalculatedFromBucket(mean = 5.0, variance = 1.0, offerPT = 6.0)
            .withCalculationParameters(CalculationParameters(dailyRate = 800.0, salesSurcharge = 0.1))
        assertEquals(6.0, assigned.offerPT, 1e-9)
        assertEquals(6.0 * 800.0, assigned.cost, 1e-9)
        assertEquals(6.0 * 800.0 * 1.1, assigned.offerPrice, 1e-9)
    }
}
