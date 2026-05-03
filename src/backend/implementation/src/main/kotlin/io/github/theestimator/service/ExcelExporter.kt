package io.github.theestimator.service

import io.github.theestimator.domain.*
import jakarta.enterprise.context.ApplicationScoped
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream

@ApplicationScoped
class ExcelExporter {

    fun export(version: EstimationVersion, output: OutputStream) {
        val workbook = XSSFWorkbook()

        writeProjectStructurePlan(workbook, version)
        writeAdditionalCosts(workbook, version)
        writePhases(workbook, version)
        writeParameters(workbook, version)
        writeEffortDrivers(workbook, version)

        workbook.write(output)
        workbook.close()
    }

    private fun writeProjectStructurePlan(workbook: XSSFWorkbook, version: EstimationVersion) {
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
        val fixedGroups = version.itemGroups.filter { group ->
            group.items.none { it is TimeRelativeEstimationItem }
        }
        val timeRelativeGroups = version.itemGroups.filter { group ->
            group.items.any { it is TimeRelativeEstimationItem }
        }

        for (group in fixedGroups) {
            val groupRow = sheet.createRow(rowIdx++)
            groupRow.createCell(0).setCellValue(group.title)
            val groupMean = group.items.sumOf { it.mean ?: 0.0 }
            val groupVariance = group.items.sumOf { it.variance ?: 0.0 }
            val groupOfferPT = group.items.sumOf { it.offerPT ?: 0.0 }
            groupRow.createCell(5).setCellValue(groupMean)
            groupRow.createCell(7).setCellValue(groupVariance)
            groupRow.createCell(11).setCellValue(groupOfferPT)
            group.phase?.abbreviation?.let { groupRow.createCell(14).setCellValue(it) }

            for (item in group.items) {
                val itemRow = sheet.createRow(rowIdx++)
                itemRow.createCell(0).setCellValue(item.description)
                item.minEffort?.let { itemRow.createCell(1).setCellValue(it) }
                item.expectedEffort?.let { itemRow.createCell(2).setCellValue(it) }
                item.maxEffort?.let { itemRow.createCell(3).setCellValue(it) }
                item.mean?.let { itemRow.createCell(4).setCellValue(it) }
                item.variance?.let { itemRow.createCell(6).setCellValue(it) }
                item.riskSurcharge?.let { itemRow.createCell(8).setCellValue(it) }
                item.driverSurcharge?.let { itemRow.createCell(9).setCellValue(it) }
                item.offerPT?.let { itemRow.createCell(10).setCellValue(it) }
                item.cost?.let { itemRow.createCell(12).setCellValue(it) }
                item.offerPrice?.let { itemRow.createCell(13).setCellValue(it) }
                item.assumptions?.let { itemRow.createCell(15).setCellValue(it) }
            }
            rowIdx++ // empty row between groups
        }

        if (timeRelativeGroups.isNotEmpty()) {
            rowIdx++
            val trHeaderRow = sheet.createRow(rowIdx++)
            trHeaderRow.createCell(0).setCellValue("Aufwände relativ zur Zeit")

            for (group in timeRelativeGroups) {
                val groupRow = sheet.createRow(rowIdx++)
                groupRow.createCell(0).setCellValue(group.title)

                for (item in group.items) {
                    val itemRow = sheet.createRow(rowIdx++)
                    itemRow.createCell(0).setCellValue(item.description)
                    item.minEffort?.let { itemRow.createCell(1).setCellValue(it) }
                    item.expectedEffort?.let { itemRow.createCell(2).setCellValue(it) }
                    item.maxEffort?.let { itemRow.createCell(3).setCellValue(it) }
                    item.mean?.let { itemRow.createCell(4).setCellValue(it) }
                    item.variance?.let { itemRow.createCell(6).setCellValue(it) }
                }
            }
        }
    }

    private fun writeAdditionalCosts(workbook: XSSFWorkbook, version: EstimationVersion) {
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
            cost.phase?.abbreviation?.let { row.createCell(2).setCellValue(it) }
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
            cost.phase?.abbreviation?.let { row.createCell(2).setCellValue(it) }
            val totalWeeks = cost.phase?.durationWeeks ?: 0.0
            row.createCell(3).setCellValue((cost.amountPerWeek ?: cost.amount) * totalWeeks)
        }
    }

    private fun writePhases(workbook: XSSFWorkbook, version: EstimationVersion) {
        val sheet = workbook.createSheet("Pakete")
        val headerRow = sheet.createRow(0)
        listOf("Name", "Kürzel", "Laufzeit\nWochen", "Aufwand\nPT", "Kosten Entwicklung",
            "Zusatzkosten\neinmalig", "Zusatzkosten\nlaufend", "Angebotspreis\nmit VT-Zuschlag"
        ).forEachIndexed { idx, h -> headerRow.createCell(idx).setCellValue(h) }

        val salesSurcharge = version.parameters.find { it.name == "Vertriebszuschlag" }?.value ?: 0.1
        val dailyRate = version.parameters.find { it.name == "Tagessatz" }?.value ?: 800.0

        version.phases.forEachIndexed { idx, phase ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(phase.name)
            row.createCell(1).setCellValue(phase.abbreviation)
            phase.durationWeeks?.let { row.createCell(2).setCellValue(it) }

            val phaseItems = version.itemGroups
                .filter { it.phase?.id == phase.id }
                .flatMap { it.items }
            val effortPT = phaseItems.sumOf { it.offerPT ?: 0.0 }
            val devCost = effortPT * dailyRate
            val oneTimeCosts = version.additionalCosts
                .filter { it.phase?.id == phase.id && it.type == AdditionalCostType.ONE_TIME }
                .sumOf { it.amount }
            val recurringCosts = version.additionalCosts
                .filter { it.phase?.id == phase.id && it.type == AdditionalCostType.RECURRING }
                .sumOf { (it.amountPerWeek ?: it.amount) * (phase.durationWeeks ?: 0.0) }

            row.createCell(3).setCellValue(effortPT)
            row.createCell(4).setCellValue(devCost)
            row.createCell(5).setCellValue(oneTimeCosts)
            row.createCell(6).setCellValue(recurringCosts)
            row.createCell(7).setCellValue((devCost + oneTimeCosts + recurringCosts) * (1 + salesSurcharge))
        }
    }

    private fun writeParameters(workbook: XSSFWorkbook, version: EstimationVersion) {
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

    private fun writeEffortDrivers(workbook: XSSFWorkbook, version: EstimationVersion) {
        val sheet = workbook.createSheet("Aufwandstreiber")
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("Aufwandstreiber Beschreibung")
        headerRow.createCell(1).setCellValue("Faktor")
        headerRow.createCell(2).setCellValue("Zusatzaufwand")
        headerRow.createCell(3).setCellValue("Kommentar")

        val totalMean = version.itemGroups.flatMap { it.items }.sumOf { it.mean ?: 0.0 }

        version.effortDrivers.forEachIndexed { idx, driver ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(driver.description)
            row.createCell(1).setCellValue(driver.factor)
            row.createCell(2).setCellValue(totalMean * driver.factor)
            driver.comment?.let { row.createCell(3).setCellValue(it) }
        }
    }
}
