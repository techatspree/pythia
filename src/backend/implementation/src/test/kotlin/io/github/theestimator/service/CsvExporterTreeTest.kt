package io.github.theestimator.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class CsvExporterTreeTest {

    private val exporter = CsvExporter()

    @Test
    fun `three-level tree emits Path column first and Node type column last`() {
        val version = TreeFixtures.threeLevel()
        val out = ByteArrayOutputStream()
        exporter.export(version, out)
        val csv = out.toByteArray().toString(Charsets.UTF_8)
        val lines = csv.trim().lines()

        val headers = lines[0].split(",")
        assertEquals("Path", headers.first(), "Path must be the leading column")
        assertEquals("Node type", headers.last(), "Node type must be the trailing column")
        assertEquals("Group", headers[1], "Group column kept for one task — task-055 removes it")

        val pathCol = 0
        val groupCol = 1
        val typeCol = headers.size - 1

        // Tree-ordered data rows (skip header, exclude totals).
        val dataRows = lines.drop(1).dropLast(1)
        assertEquals(5, dataRows.size, "expected 5 nodes in tree order")

        val expected = listOf(
            Triple("Backend",                          "",        "GROUP"),
            Triple("Backend/Auth",                     "Backend", "GROUP"),
            Triple("Backend/Auth/Token endpoint",      "Auth",    "FIXED"),
            Triple("Backend/Auth/Session storage",     "Auth",    "TIME_RELATIVE"),
            Triple("Backend/Health endpoint",          "Backend", "FIXED")
        )
        dataRows.zip(expected).forEachIndexed { idx, (line, exp) ->
            val cells = line.split(",")
            val (path, group, type) = exp
            assertEquals(path, cells[pathCol], "row $idx Path")
            assertEquals(group, cells[groupCol], "row $idx Group")
            assertEquals(type, cells[typeCol], "row $idx Node type")
        }

        // Totals row: the total must remain in the OfferPT column (not the
        // last cell). Last cell is the empty Node type slot.
        val totalsCells = lines.last().split(",")
        val offerPtCol = headers.indexOf("OfferPT")
        assertEquals("Total", totalsCells[groupCol])
        assertEquals(7.0, totalsCells[offerPtCol].toDouble(), 0.001)
        assertTrue(totalsCells.last().isEmpty(), "totals row's last cell (Node type) is empty")
    }
}
