package io.github.theestimator.service

import io.github.theestimator.domain.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EstimationCalculatorTest {

    private lateinit var calculator: EstimationCalculator

    @BeforeEach
    fun setup() {
        calculator = EstimationCalculator()
    }

    @Test
    fun `PERT mean calculates correctly`() {
        val mean = calculator.pertMean(1.0, 2.0, 3.0)
        assertEquals(2.0, mean, 0.0001)
    }

    @Test
    fun `PERT mean with skewed values`() {
        val mean = calculator.pertMean(1.0, 3.0, 5.0)
        assertEquals(3.0, mean, 0.0001)
    }

    @Test
    fun `PERT variance calculates correctly`() {
        val variance = calculator.pertVariance(1.0, 3.0)
        assertEquals(0.1111, variance, 0.001)
    }

    @Test
    fun `calculate populates all derived fields`() {
        val version = createTestVersion()
        calculator.calculate(version)

        val items = version.itemGroups.flatMap { it.items }
        items.forEach { item ->
            assertNotNull(item.mean)
            assertNotNull(item.variance)
            assertNotNull(item.riskSurcharge)
            assertNotNull(item.driverSurcharge)
            assertNotNull(item.offerPT)
            assertNotNull(item.cost)
            assertNotNull(item.offerPrice)
        }
    }

    @Test
    fun `calculate produces correct mean for item`() {
        val version = createTestVersion()
        calculator.calculate(version)

        val item = version.itemGroups[0].items[0]
        // (1 + 4*2 + 3) / 6 = 12/6 = 2.0
        assertEquals(2.0, item.mean!!, 0.0001)
    }

    @Test
    fun `calculate produces correct variance for item`() {
        val version = createTestVersion()
        calculator.calculate(version)

        val item = version.itemGroups[0].items[0]
        // ((3-1)/6)^2 = (2/6)^2 = 0.1111
        assertEquals(0.1111, item.variance!!, 0.001)
    }

    @Test
    fun `offerPT includes risk and driver surcharges`() {
        val version = createTestVersion()
        calculator.calculate(version)

        val item = version.itemGroups[0].items[0]
        assertTrue(item.offerPT!! > item.mean!!)
    }

    @Test
    fun `invariants pass for calculated version`() {
        val version = createTestVersion()
        calculator.calculate(version)

        val results = calculator.validateInvariants(version)
        results.forEach { result ->
            assertTrue(result.passed, "Invariant failed: ${result.description}, diff=${result.difference}")
        }
    }

    @Test
    fun `cost equals offerPT times daily rate`() {
        val version = createTestVersion()
        calculator.calculate(version)

        val item = version.itemGroups[0].items[0]
        assertEquals(item.offerPT!! * 800.0, item.cost!!, 0.01)
    }

    @Test
    fun `offerPrice includes sales surcharge`() {
        val version = createTestVersion()
        calculator.calculate(version)

        val item = version.itemGroups[0].items[0]
        assertEquals(item.cost!! * 1.1, item.offerPrice!!, 0.01)
    }

    private fun createTestVersion(): EstimationVersion {
        val version = EstimationVersion().apply {
            versionNumber = 1
            status = EstimationVersionStatus.DRAFT
        }

        version.parameters.addAll(listOf(
            EstimationParameter().apply { name = "Standardabweichungsfaktor"; value = 2.0; this.version = version },
            EstimationParameter().apply { name = "Tagessatz"; value = 800.0; this.version = version },
            EstimationParameter().apply { name = "Vertriebszuschlag"; value = 0.1; this.version = version }
        ))

        version.effortDrivers.add(EffortDriver().apply {
            description = "Gewährleistung"; factor = 0.05; this.version = version
        })

        val phase = ProjectPhase().apply {
            name = "Phase 1"; abbreviation = "P1"; durationWeeks = 9.0; this.version = version
        }
        version.phases.add(phase)

        val group = EstimationItemGroup().apply {
            title = "U01: Test Feature"; this.phase = phase; this.version = version
        }
        version.itemGroups.add(group)

        group.items.addAll(listOf(
            FixedEstimationItem().apply {
                description = "Task A"; minEffort = 1.0; expectedEffort = 2.0; maxEffort = 3.0
                this.phase = phase; this.group = group
            },
            FixedEstimationItem().apply {
                description = "Task B"; minEffort = 2.0; expectedEffort = 3.0; maxEffort = 5.0
                this.phase = phase; this.group = group
            }
        ))

        return version
    }
}
