package io.pythia.model

import io.pythia.method.threepoint.FixedEstimationItem
import io.pythia.method.threepoint.TimeRelativeEstimationItem
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

    // The inputs are HOURS per week (task-213): until then these tests asserted
    // hours × weeks as person-days (16.0). Now that is divided by the hours of a
    // working day, 8 by default.

    @Test
    fun `mean scales by durationWeeks and converts hours to person-days`() {
        // PERT(2,4,6) = (2 + 16 + 6)/6 = 4.0 h/week, × 4 weeks = 16 h, ÷ 8 h/day = 2.0 PT
        assertEquals(2.0, trItem().mean, delta)
    }

    @Test
    fun `variance scales by durationWeeks squared and hoursPerDay squared`() {
        // PertCalc.variance(2,6) = ((6-2)/6)² = (4/9), × 16 weeks² ÷ 64 (h/day)²
        val expected = PertCalculation.variance(2.0, 6.0) * 16.0 / 64.0
        assertEquals(expected, trItem().variance, delta)
    }

    @Test
    fun `offerPT equals scaled mean when riskFactor=0 and no drivers`() {
        val params = CalculationParameters(riskFactor = 0.0, totalDriverFactor = 0.0, dailyRate = 800.0, salesSurcharge = 0.1)
        val item = trItem().withCalculationParameters(params)
        assertEquals(item.mean, item.offerPT, delta)
        assertEquals(2.0, item.offerPT, delta)
    }

    @Test
    fun `the hours of a working day come from the calculation parameters`() {
        val item = trItem().withCalculationParameters(CalculationParameters(hoursPerDay = 6.0))
        assertEquals(16.0 / 6.0, item.mean, delta)
        assertEquals(PertCalculation.variance(2.0, 6.0) * 16.0 / 36.0, item.variance, delta)
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
            stdDevFactor = 2.0,
            dailyRate = 800.0,
            salesSurcharge = 0.1,
            roots = listOf(EstimationGroup(title = "G", children = listOf(tr, fixed)))
        )

        val calculated = version.calculate()
        val items = calculated.roots.flatMap { it.leaves().toList() }
        val calcTr = items.first { it.description == "TR" }
        val calcFixed = items.first { it.description == "Fixed" }

        // TR: 16 h ÷ 8 h/day = 2.0 PT (task-213); totalMean = 2.0 + 4.0 = 6.0
        // totalVariance = (4/9)*16/64 + (4/9)
        val totalMean = 6.0
        val totalVariance = PertCalculation.variance(2.0, 6.0) * 16.0 / 64.0 + PertCalculation.variance(2.0, 6.0)
        val riskFactor = PertCalculation.riskFactor(totalMean, totalVariance, 2.0)

        assertEquals(2.0 * (1 + riskFactor), calcTr.offerPT, delta)
        assertEquals(4.0 * (1 + riskFactor), calcFixed.offerPT, delta)
    }
}
