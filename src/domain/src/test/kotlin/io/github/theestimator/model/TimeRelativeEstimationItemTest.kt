package io.github.theestimator.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TimeRelativeEstimationItemTest {

    private val delta = 0.001

    private fun phase(durationWeeks: Double) = ProjectPhase(
        name = "Test Phase",
        abbreviation = "TP",
        durationWeeks = durationWeeks
    )

    private fun trItem(
        min: Double = 2.0,
        exp: Double = 4.0,
        max: Double = 6.0,
        phase: ProjectPhase? = phase(4.0)
    ) = TimeRelativeEstimationItem(
        unit = "h/Woche",
        _description = "Test",
        _minEffort = min,
        _expectedEffort = exp,
        _maxEffort = max,
        _phase = phase
    )

    private fun fixedItem(min: Double = 2.0, exp: Double = 4.0, max: Double = 6.0) =
        FixedEstimationItem(_description = "Test", _minEffort = min, _expectedEffort = exp, _maxEffort = max)

    @Test
    fun `mean scales by durationWeeks`() {
        // PERT(2,4,6) = (2 + 16 + 6)/6 = 4.0, × 4 weeks = 16.0
        assertEquals(16.0, trItem().mean, delta)
    }

    @Test
    fun `variance scales by durationWeeks squared`() {
        // PertCalc.variance(2,6) = ((6-2)/6)² = (4/9), × 16 = 64/9
        val expected = PertCalculation.variance(2.0, 6.0) * 16.0
        assertEquals(expected, trItem().variance, delta)
    }

    @Test
    fun `offerPT equals scaled mean when riskFactor=0 and no drivers`() {
        val params = CalculationParameters(riskFactor = 0.0, totalDriverFactor = 0.0, dailyRate = 800.0, salesSurcharge = 0.1)
        val item = trItem().withCalculationParameters(params)
        assertEquals(item.mean, item.offerPT, delta)
        assertEquals(16.0, item.offerPT, delta)
    }

    @Test
    fun `phase null gives mean zero and offerPT zero`() {
        val item = trItem(phase = null)
        assertEquals(0.0, item.mean, delta)
        assertEquals(0.0, item.variance, delta)
        assertEquals(0.0, item.offerPT, delta)
    }

    @Test
    fun `durationWeeks zero gives mean zero and offerPT zero`() {
        val item = trItem(phase = phase(0.0))
        assertEquals(0.0, item.mean, delta)
        assertEquals(0.0, item.offerPT, delta)
    }

    @Test
    fun `FixedEstimationItem mean is unchanged regardless of any phase`() {
        // Fixed item with same raw values must still return raw mean
        assertEquals(4.0, fixedItem().mean, delta)
    }

    @Test
    fun `version calculate combines TR and Fixed items correctly`() {
        val trPhase = ProjectPhase(name = "Phase", abbreviation = "P", durationWeeks = 4.0)
        val tr = TimeRelativeEstimationItem(unit = "h/Woche", _description = "TR", _minEffort = 2.0, _expectedEffort = 4.0, _maxEffort = 6.0, _phase = trPhase)
        val fixed = FixedEstimationItem(_description = "Fixed", _minEffort = 2.0, _expectedEffort = 4.0, _maxEffort = 6.0)

        val version = EstimationVersion(
            versionNumber = 1,
            parameters = listOf(
                EstimationParameter("Standardabweichungsfaktor", 2.0),
                EstimationParameter("Tagessatz", 800.0),
                EstimationParameter("Vertriebszuschlag", 0.1)
            ),
            itemGroups = listOf(EstimationItemGroup(title = "G", items = listOf(tr, fixed)))
        )

        val calculated = version.calculate()
        val items = calculated.itemGroups.flatMap { it.items }
        val calcTr = items.first { it.description == "TR" }
        val calcFixed = items.first { it.description == "Fixed" }

        // totalMean = 16.0 + 4.0 = 20.0
        // totalVariance = (4/9)*16 + (4/9) = (4/9)*17
        val totalMean = 20.0
        val totalVariance = PertCalculation.variance(2.0, 6.0) * 16.0 + PertCalculation.variance(2.0, 6.0)
        val riskFactor = PertCalculation.riskFactor(totalMean, totalVariance, 2.0)

        assertEquals(16.0 * (1 + riskFactor), calcTr.offerPT, delta)
        assertEquals(4.0 * (1 + riskFactor), calcFixed.offerPT, delta)
    }
}
