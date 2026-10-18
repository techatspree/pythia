package io.pythia.model

import io.pythia.StandardMethods
import io.pythia.method.threepoint.FixedEstimationItem
import io.pythia.method.threepoint.TimeRelativeEstimationItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * The tenant's hours per working day reach EVERY part of calculate() (task-213).
 *
 * The trap this pins: calculate() sums the leaves' mean and variance into the
 * risk factor BEFORE it stamps the full CalculationParameters on them. If the
 * leaves were not given hoursPerDay first, the risk factor would silently use
 * the default 8 while the per-leaf numbers used the tenant's value.
 */
class EstimationVersionHoursPerDayTest {

    private val delta = 1e-9

    @BeforeEach
    fun ensureRegistryPopulated() {
        StandardMethods.installAll()
    }

    private val phase = ProjectPhase(name = "Umsetzung", abbreviation = "UM", durationWeeks = 4.0)

    /** One fixed leaf (2/4/6 → 4.0 PT) and accompanying work 2/4/6 h/week over 4 weeks (16 h). */
    private fun version(hoursPerDay: Double) = EstimationVersion(
        versionNumber = 1,
        stdDevFactor = 2.0,
        dailyRate = 800.0,
        salesSurcharge = 0.1,
        hoursPerDay = hoursPerDay,
        roots = listOf(
            EstimationGroup(
                title = "G",
                children = listOf(
                    FixedEstimationItem(_description = "Fixed", _minEffort = 2.0, _expectedEffort = 4.0, _maxEffort = 6.0),
                    TimeRelativeEstimationItem(
                        _description = "TR",
                        _minEffort = 2.0,
                        _expectedEffort = 4.0,
                        _maxEffort = 6.0,
                        _phase = phase
                    )
                )
            )
        )
    ).calculate()

    private fun expectedFor(hoursPerDay: Double) {
        val calculated = version(hoursPerDay)
        val leaves = calculated.roots.flatMap { it.leaves().toList() }
        val tr = leaves.single { it.description == "TR" }
        val fixed = leaves.single { it.description == "Fixed" }

        val trMean = 16.0 / hoursPerDay
        val trVariance = PertCalculation.variance(2.0, 6.0) * 16.0 / (hoursPerDay * hoursPerDay)
        val riskFactor = PertCalculation.riskFactor(
            trMean + 4.0,
            trVariance + PertCalculation.variance(2.0, 6.0),
            2.0
        )

        assertEquals(trMean, tr.mean, delta)
        assertEquals(trVariance, tr.variance, delta)
        // The risk factor — computed BEFORE the full parameters — used the same value.
        assertEquals(trMean * (1 + riskFactor), tr.offerPT, delta)
        assertEquals(4.0 * (1 + riskFactor), fixed.offerPT, delta)
        assertEquals((trMean + 4.0) * (1 + riskFactor), calculated.totalEffort, delta)
    }

    @Test
    fun `the default of 8 hours converts accompanying work`() = expectedFor(8.0)

    @Test
    fun `6 hours per day is used throughout, risk factor included`() = expectedFor(6.0)
}
