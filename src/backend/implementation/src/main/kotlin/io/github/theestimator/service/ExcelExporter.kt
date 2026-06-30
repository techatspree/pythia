package io.github.theestimator.service

import io.github.theestimator.domain.AdditionalCostType
import io.github.theestimator.domain.submitted.SubmittedEstimationNode
import io.github.theestimator.domain.submitted.SubmittedEstimationVersion
import io.github.theestimator.domain.submitted.SubmittedGroupNode
import io.github.theestimator.domain.submitted.SubmittedTimeRelativeItemNode
import jakarta.enterprise.context.ApplicationScoped
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream

// The integer literals throughout are POI spreadsheet column/row indices — the
// physical layout of each exported sheet. Naming a constant per column across
// the five differently-shaped sheets would obscure the layout rather than
// clarify it (MagicNumber). writeNode emits a different cell set per node type,
// which is inherently branchy (CyclomaticComplexMethod). Both suppressed for
// this layout-only class.
@Suppress("MagicNumber", "CyclomaticComplexMethod")
@ApplicationScoped
class ExcelExporter {

    fun export(version: SubmittedEstimationVersion, output: OutputStream) {
        val workbook = XSSFWorkbook()

        writeProjectStructurePlan(workbook, version)
        writeAdditionalCosts(workbook, version)
        writePhases(workbook, version)
        writeParameters(workbook, version)
        writeEffortDrivers(workbook, version)

        workbook.write(output)
        workbook.close()
    }

    private fun collectLeaves(node: SubmittedEstimationNode): List<SubmittedEstimationNode> = when (node) {
        is SubmittedGroupNode -> node.children.flatMap { collectLeaves(it) }
        else -> listOf(node)
    }

    private fun allLeaves(version: SubmittedEstimationVersion): List<SubmittedEstimationNode> =
        version.roots.flatMap { collectLeaves(it) }

    private fun writeProjectStructurePlan(workbook: XSSFWorkbook, version: SubmittedEstimationVersion) {
        val sheet = workbook.createSheet("Projektstrukturplan")

        val headerRow = sheet.createRow(0)
        val headers = listOf(
            "Beschreibung", "Min", "Erwartet", "Max", "Mittelwert",
            "Mittelwert pro Gruppe", "Varianz", "Varianz pro Gruppe",
            "Zuschl. Risiko (PT)", "Zuschl. Aufwandstreiber (PT)",
            "Angebots-PT", "Angebots-PT pro Gruppe", "Kosten",
            "Angebots-preis", "Paket", "Annahmen, Abgrenzungen, Kommentare",
            "Node type", "Logical ID", "Unit"
        )
        headers.forEachIndexed { idx, header -> headerRow.createCell(idx).setCellValue(header) }

        var rowIdx = 1
        for (root in version.roots) {
            rowIdx = writeNode(root, depth = 0, sheet = sheet, rowIdx = rowIdx)
        }
    }

    private fun writeNode(node: SubmittedEstimationNode, depth: Int, sheet: Sheet, rowIdx: Int): Int {
        val row = sheet.createRow(rowIdx) as XSSFRow
        // XSSFRow only exposes getOutlineLevel() (reads from the underlying
        // CTRow). To set it, go through the CTRow proxy directly.
        row.ctRow.outlineLevel = depth.toShort()
        val indent = "  ".repeat(depth)
        when (node) {
            is SubmittedGroupNode -> {
                row.createCell(0).setCellValue(indent + (node.title ?: ""))
                row.createCell(5).setCellValue(node.mean)
                row.createCell(7).setCellValue(node.variance)
                row.createCell(11).setCellValue(node.offerPT)
                row.createCell(16).setCellValue("GROUP")
            }
            else -> {
                row.createCell(0).setCellValue(indent + (node.description ?: ""))
                row.createCell(1).setCellValue(node.minEffort ?: 0.0)
                row.createCell(2).setCellValue(node.expectedEffort ?: 0.0)
                row.createCell(3).setCellValue(node.maxEffort ?: 0.0)
                row.createCell(4).setCellValue(node.mean)
                row.createCell(6).setCellValue(node.variance)
                row.createCell(8).setCellValue(node.riskSurcharge)
                row.createCell(9).setCellValue(node.driverSurcharge)
                row.createCell(10).setCellValue(node.offerPT)
                row.createCell(12).setCellValue(node.cost)
                row.createCell(13).setCellValue(node.offerPrice)
                node.phaseAbbreviation?.let { row.createCell(14).setCellValue(it) }
                node.assumptions?.let { row.createCell(15).setCellValue(it) }
                row.createCell(16).setCellValue(
                    if (node is SubmittedTimeRelativeItemNode) "TIME_RELATIVE" else "FIXED"
                )
                if (node is SubmittedTimeRelativeItemNode) {
                    node.unit?.let { row.createCell(18).setCellValue(it) }
                }
            }
        }
        row.createCell(17).setCellValue(node.logicalId.toString())

        var next = rowIdx + 1
        if (node is SubmittedGroupNode) {
            for (child in node.children) {
                next = writeNode(child, depth + 1, sheet, next)
            }
        }
        return next
    }

    private fun writeAdditionalCosts(workbook: XSSFWorkbook, version: SubmittedEstimationVersion) {
        val sheet = workbook.createSheet("Zusatzkosten")
        var rowIdx = 0

        val oneTime = version.additionalCosts.filter { it.type == AdditionalCostType.ONE_TIME }
        val recurring = version.additionalCosts.filter { it.type == AdditionalCostType.RECURRING }

        sheet.createRow(rowIdx++).createCell(0).setCellValue("Einmalige Kosten")
        val otHeader = sheet.createRow(rowIdx++)
        otHeader.createCell(0).setCellValue("Beschreibung")
        otHeader.createCell(1).setCellValue("Betrag")
        otHeader.createCell(2).setCellValue("Paket")

        for (cost in oneTime) {
            val row = sheet.createRow(rowIdx++)
            row.createCell(0).setCellValue(cost.description)
            row.createCell(1).setCellValue(cost.amount)
            cost.phaseAbbreviation?.let { row.createCell(2).setCellValue(it) }
        }

        rowIdx++
        sheet.createRow(rowIdx++).createCell(0).setCellValue("Laufende Kosten")
        val rcHeader = sheet.createRow(rowIdx++)
        rcHeader.createCell(0).setCellValue("Beschreibung")
        rcHeader.createCell(1).setCellValue("Betrag pro Woche")
        rcHeader.createCell(2).setCellValue("Paket")
        rcHeader.createCell(3).setCellValue("Gesamtkosten")

        for (cost in recurring) {
            val row = sheet.createRow(rowIdx++)
            row.createCell(0).setCellValue(cost.description)
            row.createCell(1).setCellValue(cost.amountPerWeek ?: cost.amount)
            cost.phaseAbbreviation?.let { row.createCell(2).setCellValue(it) }
            val phase = version.phases.find { it.abbreviation == cost.phaseAbbreviation }
            val totalWeeks = phase?.durationWeeks ?: 0.0
            row.createCell(3).setCellValue((cost.amountPerWeek ?: cost.amount) * totalWeeks)
        }
    }

    private fun writePhases(workbook: XSSFWorkbook, version: SubmittedEstimationVersion) {
        val sheet = workbook.createSheet("Pakete")
        val headerRow = sheet.createRow(0)
        listOf("Name", "Kürzel", "Laufzeit\nWochen", "Aufwand\nPT", "Kosten Entwicklung",
            "Zusatzkosten\neinmalig", "Zusatzkosten\nlaufend", "Angebotspreis\nmit VT-Zuschlag"
        ).forEachIndexed { idx, h -> headerRow.createCell(idx).setCellValue(h) }

        val salesSurcharge = version.parameterValue("Vertriebszuschlag") ?: 0.1
        val dailyRate = version.parameterValue("Tagessatz") ?: 800.0
        val allLeaves = allLeaves(version)

        version.phases.forEachIndexed { idx, phase ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(phase.name)
            row.createCell(1).setCellValue(phase.abbreviation)
            phase.durationWeeks?.let { row.createCell(2).setCellValue(it) }

            val phaseItems = allLeaves.filter { it.phaseAbbreviation == phase.abbreviation }
            val effortPT = phaseItems.sumOf { it.offerPT }
            val devCost = effortPT * dailyRate
            val oneTimeCosts = version.additionalCosts
                .filter { it.phaseAbbreviation == phase.abbreviation && it.type == AdditionalCostType.ONE_TIME }
                .sumOf { it.amount }
            val recurringCosts = version.additionalCosts
                .filter { it.phaseAbbreviation == phase.abbreviation && it.type == AdditionalCostType.RECURRING }
                .sumOf { (it.amountPerWeek ?: it.amount) * (phase.durationWeeks ?: 0.0) }

            row.createCell(3).setCellValue(effortPT)
            row.createCell(4).setCellValue(devCost)
            row.createCell(5).setCellValue(oneTimeCosts)
            row.createCell(6).setCellValue(recurringCosts)
            row.createCell(7).setCellValue((devCost + oneTimeCosts + recurringCosts) * (1 + salesSurcharge))
        }
    }

    private fun writeParameters(workbook: XSSFWorkbook, version: SubmittedEstimationVersion) {
        val sheet = workbook.createSheet("Parameter")
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("Name")
        headerRow.createCell(1).setCellValue("Wert")
        headerRow.createCell(2).setCellValue("Kommentar")

        version.parameters.forEachIndexed { idx, param ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(param.name)
            row.createCell(1).setCellValue(param.value)
            param.comment?.let { row.createCell(2).setCellValue(it) }
        }
    }

    private fun writeEffortDrivers(workbook: XSSFWorkbook, version: SubmittedEstimationVersion) {
        val sheet = workbook.createSheet("Aufwandstreiber")
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("Aufwandstreiber Beschreibung")
        headerRow.createCell(1).setCellValue("Faktor")
        headerRow.createCell(2).setCellValue("Zusatzaufwand")
        headerRow.createCell(3).setCellValue("Kommentar")

        val totalMean = allLeaves(version).sumOf { it.mean }

        version.effortDrivers.forEachIndexed { idx, driver ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(driver.description)
            row.createCell(1).setCellValue(driver.factor)
            row.createCell(2).setCellValue(totalMean * driver.factor)
            driver.comment?.let { row.createCell(3).setCellValue(it) }
        }
    }
}
