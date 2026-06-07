package io.github.theestimator.model

import io.github.theestimator.service.EstimationCalculator
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EstimationCalculatorInvariantsTest {

    private val calculator = EstimationCalculator()

    private fun item(desc: String, min: Double, exp: Double, max: Double) = FixedEstimationItem(
        _description = desc,
        _minEffort = min,
        _expectedEffort = exp,
        _maxEffort = max
    )

    private fun threeLevelVersion(): EstimationVersion {
        val a = item("a", 1.0, 2.0, 3.0)
        val b = item("b", 2.0, 4.0, 6.0)
        val c = item("c", 3.0, 6.0, 9.0)
        val d = item("d", 4.0, 8.0, 12.0)

        return EstimationVersion(
            versionNumber = 1,
            parameters = listOf(
                EstimationParameter("Standardabweichungsfaktor", 2.0),
                EstimationParameter("Tagessatz", 800.0),
                EstimationParameter("Vertriebszuschlag", 0.1)
            ),
            roots = listOf(
                EstimationGroup(title = "Backend", children = listOf(
                    a,
                    EstimationGroup(title = "Auth", children = listOf(b, c))
                )),
                EstimationGroup(title = "Frontend", children = listOf(d))
            )
        )
    }

    @Test
    fun `all five invariants pass on a calculated three-level tree`() {
        val calculated = threeLevelVersion().calculate()
        val results = calculator.validateInvariants(calculated)
        for (result in results) {
            assertTrue(result.passed, "Invariant failed: ${result.description}, difference=${result.difference}")
        }
    }

    @Test
    fun `accumulation invariant detects a tampered totalEffort`() {
        val calculated = threeLevelVersion().calculate()
        val tampered = calculated.copy(totalEffort = calculated.totalEffort + 100.0)
        val results = calculator.validateInvariants(tampered)
        val totalEffortInvariant = results.first { it.description == "Gesamtaufwand = Summe aller AngebotsPT" }
        assertTrue(!totalEffortInvariant.passed, "Tampering with totalEffort should fail the first invariant")
    }
}
