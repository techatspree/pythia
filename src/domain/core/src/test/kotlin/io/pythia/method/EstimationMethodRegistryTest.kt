package io.pythia.method

import io.pythia.model.CalculationParameters
import io.pythia.model.EstimationItem
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
        override val description: String = "fake"

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
    fun leaveEmpty() {
        // No re-install here since task-143: the standard modules live in the
        // method modules, which :domain:core cannot see. That is safe because
        // each Gradle module runs its own test JVM, so an empty registry here
        // can no longer leak into another module's tests — and nothing else in
        // core's own suite calls EstimationVersion.calculate().
        EstimationMethodRegistry.clear()
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
