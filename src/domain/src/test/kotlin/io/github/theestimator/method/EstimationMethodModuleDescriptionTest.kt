package io.github.theestimator.method

import io.github.theestimator.method.bucketsampled.BucketMethodModule
import io.github.theestimator.method.threepoint.ThreePointMethodModule
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// Contract test: both concrete method modules must ship a non-blank English
// description (task-119). The wire and the popover render it verbatim; an
// empty override would surface as an empty popover.
class EstimationMethodModuleDescriptionTest {

    @Test
    fun `three-point module ships a non-blank description`() {
        val text = ThreePointMethodModule().description
        assertTrue(text.trim().length >= 20, "PERT description too short: '$text'")
    }

    @Test
    fun `bucket-sampled module ships a non-blank description`() {
        val text = BucketMethodModule().description
        assertTrue(text.trim().length >= 20, "Bucket description too short: '$text'")
    }
}
