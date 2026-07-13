package io.github.theestimator.model

import io.github.theestimator.method.threepoint.FixedEstimationItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EstimationNodeTest {

    private val delta = 0.001

    private fun item(min: Double = 2.0, exp: Double = 4.0, max: Double = 6.0, id: String = newId()) =
        FixedEstimationItem(
            _description = "Item",
            _minEffort = min,
            _expectedEffort = exp,
            _maxEffort = max,
            _logicalId = id
        )

    @Test
    fun `group mean equals sum of leaf means`() {
        val a = item(min = 1.0, exp = 2.0, max = 3.0) // mean = 2.0
        val b = item(min = 4.0, exp = 5.0, max = 6.0) // mean = 5.0
        val group = EstimationGroup(title = "G", children = listOf(a, b))
        assertEquals(a.mean + b.mean, group.mean, delta)
        assertEquals(7.0, group.mean, delta)
    }

    @Test
    fun `three-level tree accumulates across all depths`() {
        val leafA = item(min = 1.0, exp = 2.0, max = 3.0)
        val leafB = item(min = 2.0, exp = 4.0, max = 6.0)
        val leafC = item(min = 3.0, exp = 6.0, max = 9.0)

        val inner = EstimationGroup(title = "inner", children = listOf(leafA, leafB))
        val root  = EstimationGroup(title = "root",  children = listOf(inner, leafC))

        val expectedMean     = leafA.mean + leafB.mean + leafC.mean
        val expectedVariance = leafA.variance + leafB.variance + leafC.variance
        val expectedOfferPT  = leafA.offerPT + leafB.offerPT + leafC.offerPT
        val expectedCost     = leafA.cost + leafB.cost + leafC.cost

        assertEquals(expectedMean, root.mean, delta)
        assertEquals(expectedVariance, root.variance, delta)
        assertEquals(expectedOfferPT, root.offerPT, delta)
        assertEquals(expectedCost, root.cost, delta)
    }

    @Test
    fun `withCalculationParameters propagates to the deepest leaf`() {
        val params = CalculationParameters(riskFactor = 0.3, totalDriverFactor = 0.1, dailyRate = 700.0, salesSurcharge = 0.2)

        val deepLeaf = item()
        val mid = EstimationGroup(title = "mid", children = listOf(deepLeaf))
        val root = EstimationGroup(title = "root", children = listOf(mid))

        val recalculated = root.withCalculationParameters(params)

        assertNotSame(root, recalculated)
        val deepReplacement = (recalculated.children.first() as EstimationGroup).children.first() as EstimationItem
        assertEquals(params, deepReplacement.calculationParameters)
    }

    @Test
    fun `leaves walks all leaves in depth-first order`() {
        val a = item(id = "a")
        val b = item(id = "b")
        val c = item(id = "c")
        val tree = EstimationGroup(
            title = "root",
            children = listOf(
                EstimationGroup(title = "left", children = listOf(a, b)),
                c
            )
        )
        val ids = tree.leaves().map { it.logicalId }.toList()
        assertEquals(listOf("a", "b", "c"), ids)
    }

    @Test
    fun `data class equality is structural over children`() {
        val l1 = item(id = "x")
        val l2 = item(id = "x")
        val g1 = EstimationGroup(title = "G", children = listOf(l1), _logicalId = "g")
        val g2 = EstimationGroup(title = "G", children = listOf(l2), _logicalId = "g")
        assertEquals(g1, g2)
        assertTrue(g1.hashCode() == g2.hashCode())
    }
}
