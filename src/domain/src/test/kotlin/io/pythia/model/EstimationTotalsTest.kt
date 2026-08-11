package io.pythia.model

import io.pythia.StandardMethods
import io.pythia.method.bucketsampled.BucketedEstimationItem
import io.pythia.method.threepoint.FixedEstimationItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EstimationTotalsTest {

    private val delta = 1e-9

    @BeforeEach
    fun ensureRegistryPopulated() {
        // The aggregator's suite can see every method, so it installs the real
        // standard set (the registry no longer self-populates — task-143).
        StandardMethods.installAll()
    }

    private fun item(desc: String, min: Double, exp: Double, max: Double) = FixedEstimationItem(
        _description = desc,
        _minEffort = min,
        _expectedEffort = exp,
        _maxEffort = max
    )

    private fun versionWith(
        roots: List<EstimationNode>,
        additionalCosts: List<AdditionalCost> = emptyList()
    ) = EstimationVersion(
        versionNumber = 1,
        stdDevFactor = 2.0,
        dailyRate = 800.0,
        salesSurcharge = 0.1,
        additionalCosts = additionalCosts,
        roots = roots
    )

    @Test
    fun `totals derive the money figures from offerPT, the daily rate and the sales surcharge`() {
        val version = versionWith(listOf(item("a", 1.0, 2.0, 3.0), item("b", 2.0, 4.0, 6.0)))

        val calculated = version.calculate()
        val totals = calculated.totals()

        assertEquals(calculated.roots.sumOf { it.offerPT }, totals.offerPT, delta)
        assertEquals(calculated.totalEffort, totals.offerPT, delta)
        assertEquals(totals.offerPT * 800.0, totals.developmentCost, delta)
        assertEquals(totals.developmentCost * 1.1, totals.totalOfferPrice, delta)
        // With no additional costs the two prices coincide.
        assertEquals(totals.totalOfferPrice, totals.developmentOfferPrice, delta)
        assertEquals(0.0, totals.additionalOneTime, delta)
        assertEquals(0.0, totals.additionalRecurring, delta)
        assertEquals(2, totals.leafCount)
    }

    @Test
    fun `totals count each leaf of a grouped tree exactly once`() {
        val leaves = listOf(
            item("a", 1.0, 2.0, 3.0),
            item("b", 2.0, 4.0, 6.0),
            item("c", 3.0, 6.0, 9.0)
        )
        val version = versionWith(listOf(
            EstimationGroup(title = "outer", children = listOf(
                leaves[0],
                EstimationGroup(title = "inner", children = listOf(leaves[1], leaves[2]))
            ))
        ))

        val calculated = version.calculate()
        val totals = calculated.totals()

        // Summing every NODE (groups included) would double-count the subtree —
        // the defect the estimation grid's footer had. Totals are leaf-based.
        val leafOfferPT = calculated.roots.flatMap { it.leaves().toList() }.sumOf { it.offerPT }
        assertEquals(3, totals.leafCount)
        assertEquals(leafOfferPT, totals.offerPT, delta)
        assertEquals(leafOfferPT, calculated.roots.sumOf { it.offerPT }, delta)
    }

    @Test
    fun `totals of a bucket estimation include the non-samples' inherited bucket means`() {
        val sampleOne = BucketedEstimationItem(
            bucketId = "b1", isSample = true, optimistic = 1.0, likely = 2.0, pessimistic = 3.0
        )
        val sampleTwo = BucketedEstimationItem(
            bucketId = "b1", isSample = true, optimistic = 3.0, likely = 6.0, pessimistic = 9.0
        )
        val nonSample = BucketedEstimationItem(bucketId = "b1", isSample = false)

        val samplesOnly = versionWith(listOf(sampleOne, sampleTwo)).calculate().totals()
        val withNonSample = versionWith(listOf(sampleOne, sampleTwo, nonSample)).calculate().totals()

        assertEquals(3, withNonSample.leafCount)
        assertTrue(
            withNonSample.meanPT > samplesOnly.meanPT,
            "the non-sample must contribute its inherited bucket mean"
        )
        // Its bucket mean is the average of the two samples' means (2.0 and 6.0).
        assertEquals(samplesOnly.meanPT + 4.0, withNonSample.meanPT, delta)
    }

    @Test
    fun `additional costs add a one-time amount and a recurring amount per phase week`() {
        val phase = ProjectPhase(name = "Build", abbreviation = "B", durationWeeks = 4.0)
        val version = versionWith(
            roots = listOf(item("a", 1.0, 2.0, 3.0)),
            additionalCosts = listOf(
                AdditionalCost(description = "licence", amount = 500.0, type = AdditionalCostType.ONE_TIME),
                AdditionalCost(
                    description = "hosting",
                    type = AdditionalCostType.RECURRING,
                    amountPerWeek = 100.0,
                    phase = phase
                )
            )
        )

        val totals = version.calculate().totals()

        assertEquals(500.0, totals.additionalOneTime, delta)
        assertEquals(400.0, totals.additionalRecurring, delta)
        // The tree's own price excludes the additional costs, so a grid column
        // of per-leaf offer prices still adds up to it.
        assertEquals(totals.developmentCost * 1.1, totals.developmentOfferPrice, delta)
        val net = totals.developmentCost + totals.additionalOneTime + totals.additionalRecurring
        assertEquals(net * 1.1, totals.totalOfferPrice, delta)
        // The displayed lines must add up to the headline price.
        assertEquals(totals.totalOfferPrice - net, totals.salesSurchargeAmount, delta)
        assertEquals(0, totals.recurringWithoutPhase)
    }

    @Test
    fun `a recurring cost without a phase contributes zero and is counted, a zero-week phase is not`() {
        val zeroWeeks = ProjectPhase(name = "Kickoff", abbreviation = "K", durationWeeks = 0.0)
        val version = versionWith(
            roots = listOf(item("a", 1.0, 2.0, 3.0)),
            additionalCosts = listOf(
                AdditionalCost(
                    description = "unassigned hosting",
                    type = AdditionalCostType.RECURRING,
                    amountPerWeek = 100.0
                ),
                AdditionalCost(
                    description = "kickoff hosting",
                    type = AdditionalCostType.RECURRING,
                    amountPerWeek = 100.0,
                    phase = zeroWeeks
                )
            )
        )

        val totals = version.calculate().totals()

        assertEquals(0.0, totals.additionalRecurring, delta)
        assertEquals(1, totals.recurringWithoutPhase)
    }
}
