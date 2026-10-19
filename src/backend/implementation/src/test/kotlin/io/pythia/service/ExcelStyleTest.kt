package io.pythia.service

import io.pythia.StandardMethods
import io.pythia.domain.EstimationBucket
import io.pythia.method.EstimationMethod
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID

// The exported workbook's minimal visual design (task-185): header bands, a
// frozen header row, explicit widths, number formats by column meaning, bold
// group rows. Styles must never change a cell's value or type — the numeric
// assertions below are what keep the import contract honest.
class ExcelStyleTest {

    @BeforeEach
    fun installMethods() = StandardMethods.installAll()

    private fun export(method: EstimationMethod = EstimationMethod.THREE_POINT_PERT,
                       buckets: List<EstimationBucket> = emptyList(),
                       version: io.pythia.domain.submitted.SubmittedEstimationVersion =
                           TreeFixtures.threeLevel().apply { hoursPerDay = 8.0 }): XSSFWorkbook {
        val out = ByteArrayOutputStream()
        ExcelExporter().export(version, method, buckets, out)
        return XSSFWorkbook(ByteArrayInputStream(out.toByteArray()))
    }

    private fun XSSFWorkbook.isBold(cell: Cell) = getFontAt(cell.cellStyle.fontIndex).bold

    private fun XSSFSheet.cell(row: Int, col: Int): Cell = getRow(row).getCell(col)

    @Test
    fun `every header band is bold and wraps, and Zusatzkosten keeps its section titles plain`() {
        export().use { wb ->
            // Header row per sheet: row 0, except Zusatzkosten (bands at rows 1 and 4).
            val headerRows = mapOf(
                "Projektstrukturplan" to listOf(0),
                "Zusatzkosten" to listOf(1, 4),
                "Pakete" to listOf(0),
                "Parameter" to listOf(0),
                "Aufwandstreiber" to listOf(0)
            )
            assertEquals(headerRows.keys, (0 until wb.numberOfSheets).map { wb.getSheetName(it) }.toSet())
            for ((name, rows) in headerRows) {
                val sheet = wb.getSheet(name)
                for (r in rows) {
                    val cell = sheet.cell(r, 0)
                    assertTrue(wb.isBold(cell), "$name row $r header is bold")
                    assertTrue(cell.cellStyle.wrapText, "$name row $r header wraps")
                }
            }
            val costs = wb.getSheet("Zusatzkosten")
            for (r in listOf(0, 3)) {
                val title = costs.cell(r, 0)
                assertTrue(wb.isBold(title), "section title row $r is bold")
                assertFalse(title.cellStyle.wrapText, "section title row $r is not a header band")
            }
        }
    }

    @Test
    fun `the structure plan freezes its header and Zusatzkosten freezes nothing`() {
        export().use { wb ->
            val pane = wb.getSheet("Projektstrukturplan").paneInformation
            assertTrue(pane != null && pane.isFreezePane, "header row is frozen")
            assertEquals(1, pane!!.horizontalSplitPosition.toInt())
            assertNull(wb.getSheet("Zusatzkosten").paneInformation, "two header bands — no freeze pane")
        }
    }

    @Test
    fun `columns have explicit widths, the label column widest`() {
        export().use { wb ->
            val sheet = wb.getSheet("Projektstrukturplan")
            assertTrue(sheet.getColumnWidth(0) > sheet.getColumnWidth(1))
            assertEquals(52 * 256, sheet.getColumnWidth(0))
        }
    }

    @Test
    fun `group rows are bold, leaf rows are not`() {
        export().use { wb ->
            val sheet = wb.getSheet("Projektstrukturplan")
            // Row 1 Backend (group), row 3 Token endpoint (leaf).
            assertTrue(wb.isBold(sheet.cell(1, 0)), "group label is bold")
            assertFalse(wb.isBold(sheet.cell(3, 0)), "leaf label is regular")
        }
    }

    @Test
    fun `money, percentages and person-days carry their formats, values unchanged`() {
        export().use { wb ->
            val plan = wb.getSheet("Projektstrukturplan")
            val cost = plan.cell(3, 12)
            assertTrue(cost.cellStyle.dataFormatString.contains("€"), "Kosten is money")
            val offerPrice = plan.cell(3, 13)
            assertEquals(CellType.NUMERIC, offerPrice.cellType)
            assertEquals(1760.0, offerPrice.numericCellValue, 1e-9, "Angebots-preis value unchanged")
            assertEquals("#,##0.00", plan.cell(3, 4).cellStyle.dataFormatString, "mean is person-days")

            val params = wb.getSheet("Parameter")
            val sales = (1..params.lastRowNum).map { params.getRow(it) }
                .single { it.getCell(0).stringCellValue == ExcelGermanLabels.Parameters.SALES_SURCHARGE }
            assertTrue(sales.getCell(1).cellStyle.dataFormatString.contains("%"), "Vertriebszuschlag is a percentage")
            val hours = (1..params.lastRowNum).map { params.getRow(it) }
                .single { it.getCell(0).stringCellValue == ExcelGermanLabels.Parameters.HOURS_PER_DAY }
            assertEquals("#,##0.00", hours.getCell(1).cellStyle.dataFormatString, "Stunden pro Arbeitstag")
            assertEquals(8.0, hours.getCell(1).numericCellValue, 1e-9)
        }
    }

    @Test
    fun `in a bucket workbook only the numeric method cells get a number format`() {
        val bucketA = EstimationBucket().apply { id = UUID.randomUUID(); label = "S"; position = 0 }
        val bucketB = EstimationBucket().apply { id = UUID.randomUUID(); label = "M"; position = 1 }
        export(EstimationMethod.BUCKET_SAMPLED_PERT, listOf(bucketA, bucketB), TreeFixtures.bucketed(bucketA, bucketB))
            .use { wb ->
                val plan = wb.getSheet("Projektstrukturplan")
                // Row 2 is the sample leaf; columns 1..5 are the method block.
                val block = (1..5).mapNotNull { plan.getRow(2).getCell(it) }
                val text = block.filter { it.cellType == CellType.STRING }
                val numbers = block.filter { it.cellType == CellType.NUMERIC }
                assertTrue(text.isNotEmpty() && numbers.isNotEmpty(), "the block mixes text and numbers")
                text.forEach { assertEquals("General", it.cellStyle.dataFormatString, "text cell ${it.columnIndex}") }
                numbers.forEach { assertEquals("#,##0.00", it.cellStyle.dataFormatString, "number cell ${it.columnIndex}") }
                assertEquals("0", wb.getSheet("Eimer").cell(1, 1).cellStyle.dataFormatString, "Eimer position")
            }
    }
}
