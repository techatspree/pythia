package io.github.theestimator.service

import io.github.theestimator.domain.submitted.SubmittedEstimationVersion
import io.github.theestimator.domain.submitted.SubmittedFixedItemNode
import io.github.theestimator.domain.submitted.SubmittedGroupNode
import io.github.theestimator.domain.submitted.SubmittedTimeRelativeItemNode
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID

class ExcelExporterTreeTest {

    private val exporter = ExcelExporter()

    @Test
    fun `three-level tree is written in tree order with outline level and node-type column`() {
        val version = TreeFixtures.threeLevel()

        val out = ByteArrayOutputStream()
        exporter.export(version, out)

        val workbook = XSSFWorkbook(ByteArrayInputStream(out.toByteArray()))
        val sheet = workbook.getSheet("Projektstrukturplan")

        // Header at row 0: Node type at col 16, Logical ID at col 17.
        val headerRow = sheet.getRow(0)
        assertEquals("Node type", headerRow.getCell(16).stringCellValue)
        assertEquals("Logical ID", headerRow.getCell(17).stringCellValue)
        assertEquals("Unit", headerRow.getCell(18).stringCellValue)

        // Rows in tree order: Backend, Auth, Token, Session, Health.
        val rows = (1..sheet.lastRowNum).mapNotNull { sheet.getRow(it) as? XSSFRow }
        assertEquals(5, rows.size, "expected 5 rows (1 root group + 1 nested group + 3 leaves)")

        val rowAssertions = listOf(
            Triple(0, "GROUP", "Backend"),
            Triple(1, "GROUP", "Auth"),
            Triple(2, "FIXED", "Token endpoint"),
            Triple(2, "TIME_RELATIVE", "Session storage"),
            Triple(1, "FIXED", "Health endpoint")
        )

        rows.zip(rowAssertions).forEachIndexed { idx, (row, expected) ->
            val (expectedLevel, expectedType, expectedLabel) = expected
            assertEquals(expectedLevel, row.outlineLevel, "row $idx outline level")
            assertEquals(expectedType, row.getCell(16).stringCellValue, "row $idx node type")
            val label = row.getCell(0).stringCellValue
            assertEquals("  ".repeat(expectedLevel) + expectedLabel, label, "row $idx column-0 label")
        }

        // Group rows carry accumulated values in cols 5 (mean), 7 (variance),
        // 11 (offerPT). Backend.offerPT = Auth.offerPT + Health.offerPT.
        val backendRow = rows[0]
        val authRow = rows[1]
        val healthRow = rows[4]
        assertEquals("GROUP", backendRow.getCell(16).stringCellValue)
        assertEquals(authRow.getCell(11).numericCellValue + healthRow.getCell(10).numericCellValue,
            backendRow.getCell(11).numericCellValue, 0.001)

        // The TIME_RELATIVE leaf carries the unit in col 18.
        val sessionRow = rows[3]
        assertEquals("h/Woche", sessionRow.getCell(18).stringCellValue)

        // Logical IDs are written so the importer can round-trip identity.
        val rootLogicalId = backendRow.getCell(17).stringCellValue
        assertTrue(rootLogicalId.isNotBlank())
        UUID.fromString(rootLogicalId)

        workbook.close()
    }
}

internal object TreeFixtures {

    fun threeLevel(): SubmittedEstimationVersion {
        // Backend
        //   ├─ Auth
        //   │    ├─ Token endpoint (FIXED, offerPT=2)
        //   │    └─ Session storage (TIME_RELATIVE, offerPT=4, unit=h/Woche)
        //   └─ Health endpoint (FIXED, offerPT=1)
        val token = SubmittedFixedItemNode().apply {
            logicalId = UUID.randomUUID()
            description = "Token endpoint"
            minEffort = 1.0; expectedEffort = 2.0; maxEffort = 3.0
            mean = 2.0; variance = 0.1; offerPT = 2.0; cost = 1600.0; offerPrice = 1760.0
        }
        val session = SubmittedTimeRelativeItemNode().apply {
            logicalId = UUID.randomUUID()
            description = "Session storage"
            unit = "h/Woche"
            minEffort = 2.0; expectedEffort = 4.0; maxEffort = 6.0
            mean = 4.0; variance = 0.4; offerPT = 4.0; cost = 3200.0; offerPrice = 3520.0
        }
        val auth = SubmittedGroupNode().apply {
            logicalId = UUID.randomUUID()
            title = "Auth"
            mean = 6.0; variance = 0.5; offerPT = 6.0; cost = 4800.0; offerPrice = 5280.0
        }
        token.parent = auth; token.position = 0
        session.parent = auth; session.position = 1
        auth.children.addAll(listOf(token, session))

        val health = SubmittedFixedItemNode().apply {
            logicalId = UUID.randomUUID()
            description = "Health endpoint"
            minEffort = 1.0; expectedEffort = 1.0; maxEffort = 1.0
            mean = 1.0; variance = 0.0; offerPT = 1.0; cost = 800.0; offerPrice = 880.0
        }
        val backend = SubmittedGroupNode().apply {
            logicalId = UUID.randomUUID()
            title = "Backend"
            mean = 7.0; variance = 0.5; offerPT = 7.0; cost = 5600.0; offerPrice = 6160.0
        }
        auth.parent = backend; auth.position = 0
        health.parent = backend; health.position = 1
        backend.children.addAll(listOf(auth, health))

        return SubmittedEstimationVersion().apply {
            versionNumber = 1
            totalEffort = 7.0
            backend.position = 0
            roots.add(backend)
        }
    }
}
