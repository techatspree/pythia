package io.github.theestimator.service

import io.github.theestimator.domain.AdditionalCostType
import io.github.theestimator.domain.submitted.*
import jakarta.enterprise.context.ApplicationScoped
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream

// task-051 compile shim — task-053 introduces outline levels for nested trees.
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

    // For depth-1 export: pair each top-level GROUP root with the flattened
    // list of leaves in its subtree (deeper nesting collapses to a flat list).
    private fun flattenedGroups(version: SubmittedEstimationVersion): List<Pair<SubmittedGroupNode, List<SubmittedEstimationNode>>> =
        version.roots.filterIsInstance<SubmittedGroupNode>().map { g -> g to collectLeaves(g) }

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
            "Angebots-preis", "Paket", "Annahmen, Abgrenzungen, Kommentare"
        )
        headers.forEachIndexed { idx, header -> headerRow.createCell(idx).setCellValue(header) }

        var rowIdx = 1
        val groups = flattenedGroups(version)
        val fixedGroups = groups.filter { (_, items) ->
            items.none { it is SubmittedTimeRelativeItemNode }
        }
        val timeRelativeGroups = groups.filter { (_, items) ->
            items.any { it is SubmittedTimeRelativeItemNode }
        }

        for ((group, items) in fixedGroups) {
            val groupRow = sheet.createRow(rowIdx++)
            groupRow.createCell(0).setCellValue(group.title ?: "")
            val groupMean = items.sumOf { it.mean }
            val groupVariance = items.sumOf { it.variance }
            val groupOfferPT = items.sumOf { it.offerPT }
            groupRow.createCell(5).setCellValue(groupMean)
            groupRow.createCell(7).setCellValue(groupVariance)
            groupRow.createCell(11).setCellValue(groupOfferPT)
            items.firstOrNull()?.phaseAbbreviation?.let { groupRow.createCell(14).setCellValue(it) }

            for (item in items) {
                val itemRow = sheet.createRow(rowIdx++)
                itemRow.createCell(0).setCellValue(item.description ?: "")
                itemRow.createCell(1).setCellValue(item.minEffort ?: 0.0)
                itemRow.createCell(2).setCellValue(item.expectedEffort ?: 0.0)
                itemRow.createCell(3).setCellValue(item.maxEffort ?: 0.0)
                itemRow.createCell(4).setCellValue(item.mean)
                itemRow.createCell(6).setCellValue(item.variance)
                itemRow.createCell(8).setCellValue(item.riskSurcharge)
                itemRow.createCell(9).setCellValue(item.driverSurcharge)
                itemRow.createCell(10).setCellValue(item.offerPT)
                itemRow.createCell(12).setCellValue(item.cost)
                itemRow.createCell(13).setCellValue(item.offerPrice)
                item.assumptions?.let { itemRow.createCell(15).setCellValue(it) }
            }
            rowIdx++
        }

        if (timeRelativeGroups.isNotEmpty()) {
            rowIdx++
            val trHeaderRow = sheet.createRow(rowIdx++)
            trHeaderRow.createCell(0).setCellValue("Aufwände relativ zur Zeit")

            for ((group, items) in timeRelativeGroups) {
                val groupRow = sheet.createRow(rowIdx++)
                groupRow.createCell(0).setCellValue(group.title ?: "")

                for (item in items) {
                    val itemRow = sheet.createRow(rowIdx++)
                    itemRow.createCell(0).setCellValue(item.description ?: "")
                    itemRow.createCell(1).setCellValue(item.minEffort ?: 0.0)
                    itemRow.createCell(2).setCellValue(item.expectedEffort ?: 0.0)
                    itemRow.createCell(3).setCellValue(item.maxEffort ?: 0.0)
                    itemRow.createCell(4).setCellValue(item.mean)
                    itemRow.createCell(6).setCellValue(item.variance)
                }
            }
        }
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
