package io.github.theestimator.method.threepoint

import io.github.theestimator.method.EstimationMethod
import io.github.theestimator.method.EstimationMethodRegistry
import io.github.theestimator.model.CalculationParameters
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ThreePointMethodModuleTest {

    private val module = ThreePointMethodModule()
    private val delta = 0.001

    @BeforeEach
    @AfterEach
    fun reset() {
        EstimationMethodRegistry.clear()
    }

    @Test
    fun `calculate delegates to the per-leaf PERT mapping`() {
        val item = FixedEstimationItem(
            _description = "X",
            _minEffort = 2.0,
            _expectedEffort = 4.0,
            _maxEffort = 6.0
        )
        val params = CalculationParameters(
            riskFactor = 0.1,
            totalDriverFactor = 0.2,
            dailyRate = 800.0,
            salesSurcharge = 0.1
        )
        val result = module.calculate(item, params)
        // mean = (2 + 4*4 + 6) / 6 = 4.0; offerPT = mean * (1 + 0.1 + 0.2) = 5.2
        assertEquals(4.0, result.mean, delta)
        assertEquals(5.2, result.offerPT, delta)
        assertEquals(item.withCalculationParameters(params).offerPT, result.offerPT, delta)
    }

    @Test
    fun `export headers and row have matching arity`() {
        val item = FixedEstimationItem(
            _description = "X",
            _minEffort = 1.0,
            _expectedEffort = 2.0,
            _maxEffort = 3.0
        )
        assertEquals(module.exportColumnHeaders().size, module.exportRow(item).size)
    }

    @Test
    fun `installStandardMethods registers the PERT module`() {
        EstimationMethodRegistry.installStandardMethods()
        assertNotNull(EstimationMethodRegistry.get(EstimationMethod.THREE_POINT_PERT))
        assertEquals(
            EstimationMethod.THREE_POINT_PERT,
            EstimationMethodRegistry.require(EstimationMethod.THREE_POINT_PERT).method
        )
    }
}
