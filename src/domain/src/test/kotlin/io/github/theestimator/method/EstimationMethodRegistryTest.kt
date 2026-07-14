package io.github.theestimator.method

import io.github.theestimator.model.CalculationParameters
import io.github.theestimator.model.EstimationItem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EstimationMethodRegistryTest {

    // Minimal module: the registry tests only exercise register/get/require/all,
    // so calculate/export just satisfy the interface.
    private class FakeModule(override val method: EstimationMethod) : EstimationMethodModule {
        override fun calculate(item: EstimationItem, params: CalculationParameters): EstimationItem =
            item.withCalculationParameters(params)

        override fun exportRow(item: EstimationItem): List<String> = emptyList()

        override fun exportColumnHeaders(): List<String> = emptyList()
    }

    @BeforeEach
    fun startEmpty() {
        EstimationMethodRegistry.clear()
    }

    @AfterEach
    fun restoreStandard() {
        // EstimationVersion.calculate() now reads this process-wide singleton, so
        // leave the standard methods installed — otherwise a later test class (the
        // class order is non-deterministic) would run calculate() against an empty
        // registry and fail with "No estimation method module registered".
        EstimationMethodRegistry.clear()
        EstimationMethodRegistry.installStandardMethods()
    }

    @Test
    fun `register then get returns the same module`() {
        val module = FakeModule(EstimationMethod.THREE_POINT_PERT)
        EstimationMethodRegistry.register(module)
        assertSame(module, EstimationMethodRegistry.get(EstimationMethod.THREE_POINT_PERT))
    }

    @Test
    fun `get returns null for an unregistered method`() {
        assertNull(EstimationMethodRegistry.get(EstimationMethod.BUCKET_SAMPLED_PERT))
    }

    @Test
    fun `require throws for an unregistered method`() {
        assertThrows(IllegalStateException::class.java) {
            EstimationMethodRegistry.require(EstimationMethod.THREE_POINT_PERT)
        }
    }

    @Test
    fun `require returns the registered module`() {
        val module = FakeModule(EstimationMethod.BUCKET_SAMPLED_PERT)
        EstimationMethodRegistry.register(module)
        assertSame(module, EstimationMethodRegistry.require(EstimationMethod.BUCKET_SAMPLED_PERT))
    }

    @Test
    fun `all reflects order-independent membership`() {
        val pert = FakeModule(EstimationMethod.THREE_POINT_PERT)
        val bucket = FakeModule(EstimationMethod.BUCKET_SAMPLED_PERT)
        EstimationMethodRegistry.register(pert)
        EstimationMethodRegistry.register(bucket)
        val all = EstimationMethodRegistry.all()
        assertEquals(2, all.size)
        assertTrue(all.contains(pert))
        assertTrue(all.contains(bucket))
    }
}
