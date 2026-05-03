package io.github.theestimator.service

import io.github.theestimator.domain.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class ExcelImporterTest {

    private lateinit var importer: ExcelImporter
    private lateinit var calculator: EstimationCalculator

    @BeforeEach
    fun setup() {
        importer = ExcelImporter()
        calculator = EstimationCalculator()
    }

    @Test
    fun `import reference spreadsheet produces valid version`() {
        val version = importReferenceSpreadsheet()

        assertNotNull(version)
        assertEquals(1, version.versionNumber)
        assertEquals(EstimationVersionStatus.DRAFT, version.status)
    }

    @Test
    fun `import reads parameters`() {
        val version = importReferenceSpreadsheet()

        assertTrue(version.parameters.isNotEmpty())
        val stdDevFactor = version.parameters.find { it.name == "Standardabweichungsfaktor" }
        assertNotNull(stdDevFactor)
        assertEquals(2.0, stdDevFactor!!.value)

        val dailyRate = version.parameters.find { it.name == "Tagessatz" }
        assertNotNull(dailyRate)
        assertEquals(800.0, dailyRate!!.value)
    }

    @Test
    fun `import reads effort drivers`() {
        val version = importReferenceSpreadsheet()

        assertTrue(version.effortDrivers.isNotEmpty())
        val warranty = version.effortDrivers.find { it.description == "Gewährleistung" }
        assertNotNull(warranty)
        assertEquals(0.05, warranty!!.factor)
    }

    @Test
    fun `import reads phases`() {
        val version = importReferenceSpreadsheet()

        assertTrue(version.phases.isNotEmpty())
        val phase1 = version.phases.find { it.abbreviation == "P1" }
        assertNotNull(phase1)
        assertEquals("Phase 1", phase1!!.name)
    }

    @Test
    fun `import reads estimation item groups`() {
        val version = importReferenceSpreadsheet()

        assertTrue(version.itemGroups.isNotEmpty())
        val allItems = version.itemGroups.flatMap { it.items }
        assertTrue(allItems.isNotEmpty())
    }

    @Test
    fun `import reads additional costs`() {
        val version = importReferenceSpreadsheet()

        assertTrue(version.additionalCosts.isNotEmpty())
        val server = version.additionalCosts.find { it.description == "Server für Integrationstests" }
        assertNotNull(server)
        assertEquals(2567.0, server!!.amount)
        assertEquals(AdditionalCostType.ONE_TIME, server.type)
    }

    @Test
    fun `invariants pass on imported data`() {
        val version = importReferenceSpreadsheet()
        calculator.calculate(version)

        val results = calculator.validateInvariants(version)
        results.forEach { result ->
            assertTrue(result.passed, "Invariant failed: ${result.description}, diff=${result.difference}")
        }
    }

    private fun importReferenceSpreadsheet(): EstimationVersion {
        val file = File("../../planning/inputdata/reference-spreadsheet.xlsx")
        if (!file.exists()) {
            // Try absolute path as fallback
            val absFile = File(System.getProperty("user.dir") + "/../../../planning/inputdata/reference-spreadsheet.xlsx")
            return importer.import(absFile.inputStream(), createTestEstimation(), 1)
        }
        return importer.import(file.inputStream(), createTestEstimation(), 1)
    }

    private fun createTestEstimation(): Estimation {
        return Estimation().apply {
            offer = "Test Estimation"
            project = Project().apply { name = "Test Project" }
        }
    }
}
