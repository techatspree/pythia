package io.pythia.model

import io.pythia.StandardMethods
import io.pythia.method.threepoint.FixedEstimationItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EstimationVersionTreeTest {

    private val delta = 0.001

    // calculate() resolves the method module from the registry, which no longer
    // self-populates (task-143). Without this the suite only passed when another
    // class happened to install first.
    @BeforeEach
    fun installMethods() = StandardMethods.installAll()

    private fun item(desc: String, min: Double, exp: Double, max: Double) = FixedEstimationItem(
        _description = desc,
        _minEffort = min,
        _expectedEffort = exp,
        _maxEffort = max
    )

    private fun versionWith(roots: List<EstimationNode>) = EstimationVersion(
        versionNumber = 1,
        stdDevFactor = 2.0,
        dailyRate = 800.0,
        salesSurcharge = 0.1,
        roots = roots
    )

    @Test
    fun `totalEffort matches between a flat and an equivalent nested layout`() {
        val a = item("a", 1.0, 2.0, 3.0)
        val b = item("b", 2.0, 4.0, 6.0)
        val c = item("c", 3.0, 6.0, 9.0)
        val d = item("d", 4.0, 8.0, 12.0)

        val flat = versionWith(listOf(
            EstimationGroup(title = "All", children = listOf(a, b, c, d))
        ))

        val nested = versionWith(listOf(
            EstimationGroup(title = "L",
                children = listOf(
                    EstimationGroup(title = "L1", children = listOf(a, b)),
                    EstimationGroup(title = "L2", children = listOf(c, d))
                )
            )
        ))

        val flatCalc = flat.calculate()
        val nestedCalc = nested.calculate()

        assertEquals(flatCalc.totalEffort, nestedCalc.totalEffort, delta)
    }

    @Test
    fun `calculate assigns identical CalculationParameters to every leaf regardless of depth`() {
        val shallow = item("shallow", 1.0, 2.0, 3.0)
        val mid = item("mid", 2.0, 4.0, 6.0)
        val deep = item("deep", 3.0, 6.0, 9.0)

        val version = versionWith(listOf(
            shallow,
            EstimationGroup(title = "g1", children = listOf(
                mid,
                EstimationGroup(title = "g2", children = listOf(deep))
            ))
        ))

        val calculated = version.calculate()
        val leafParams = calculated.roots.flatMap { it.leaves().toList() }
            .map { it.calculationParameters }
            .toSet()

        assertEquals(1, leafParams.size)
    }
}
