package io.github.theestimator.service

import io.github.theestimator.domain.AdditionalCostType
import io.github.theestimator.domain.submitted.SubmittedEstimationNode
import io.github.theestimator.domain.submitted.SubmittedEstimationVersion
import io.github.theestimator.domain.submitted.SubmittedGroupNode
import io.github.theestimator.domain.submitted.SubmittedTimeRelativeItemNode
import io.github.theestimator.method.EstimationMethod
import io.github.theestimator.method.EstimationMethodRegistry
import io.quarkus.logging.Log
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
        Log.info("Exporting estimation ${version.estimation?.id} version ${version.versionNumber} to Excel")
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
        val sheet = workbook.createSheet(ExcelGermanLabels.Sheets.PROJECT_STRUCTURE_PLAN)

        val headerRow = sheet.createRow(0)
        // The method-specific input columns (Min/Expected/Max for PERT) are
        // sourced from the SPI module (task-098) — the module is the single
        // source of the PERT column shape across both exporters.
        val methodColumns = EstimationMethodRegistry
            .require(EstimationMethod.THREE_POINT_PERT)
            .exportColumnHeaders()
        val headers = listOf(ExcelGermanLabels.ProjectStructure.DESCRIPTION) + methodColumns + listOf(
            ExcelGermanLabels.ProjectStructure.MEAN,
            ExcelGermanLabels.ProjectStructure.MEAN_PER_GROUP,
            ExcelGermanLabels.ProjectStructure.VARIANCE,
            ExcelGermanLabels.ProjectStructure.VARIANCE_PER_GROUP,
            ExcelGermanLabels.ProjectStructure.RISK_SURCHARGE_PT,
            ExcelGermanLabels.ProjectStructure.DRIVER_SURCHARGE_PT,
            ExcelGermanLabels.ProjectStructure.OFFER_PT,
            ExcelGermanLabels.ProjectStructure.OFFER_PT_PER_GROUP,
            ExcelGermanLabels.ProjectStructure.COST,
            ExcelGermanLabels.ProjectStructure.OFFER_PRICE,
            ExcelGermanLabels.ProjectStructure.PACKAGE,
            ExcelGermanLabels.ProjectStructure.ASSUMPTIONS,
            // Round-trip technical columns — English tags, not customer labels.
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
        val sheet = workbook.createSheet(ExcelGermanLabels.Sheets.ADDITIONAL_COSTS)
        var rowIdx = 0

        val oneTime = version.additionalCosts.filter { it.type == AdditionalCostType.ONE_TIME }
        val recurring = version.additionalCosts.filter { it.type == AdditionalCostType.RECURRING }

        sheet.createRow(rowIdx++).createCell(0).setCellValue(ExcelGermanLabels.AdditionalCosts.ONE_TIME_SECTION)
        val otHeader = sheet.createRow(rowIdx++)
        otHeader.createCell(0).setCellValue(ExcelGermanLabels.AdditionalCosts.DESCRIPTION)
        otHeader.createCell(1).setCellValue(ExcelGermanLabels.AdditionalCosts.AMOUNT)
        otHeader.createCell(2).setCellValue(ExcelGermanLabels.AdditionalCosts.PACKAGE)

        for (cost in oneTime) {
            val row = sheet.createRow(rowIdx++)
            row.createCell(0).setCellValue(cost.description)
            row.createCell(1).setCellValue(cost.amount)
            cost.phaseAbbreviation?.let { row.createCell(2).setCellValue(it) }
        }

        rowIdx++
        sheet.createRow(rowIdx++).createCell(0).setCellValue(ExcelGermanLabels.AdditionalCosts.RECURRING_SECTION)
        val rcHeader = sheet.createRow(rowIdx++)
        rcHeader.createCell(0).setCellValue(ExcelGermanLabels.AdditionalCosts.DESCRIPTION)
        rcHeader.createCell(1).setCellValue(ExcelGermanLabels.AdditionalCosts.AMOUNT_PER_WEEK)
        rcHeader.createCell(2).setCellValue(ExcelGermanLabels.AdditionalCosts.PACKAGE)
        rcHeader.createCell(3).setCellValue(ExcelGermanLabels.AdditionalCosts.TOTAL_COST)

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
        val sheet = workbook.createSheet(ExcelGermanLabels.Sheets.PHASES)
        val headerRow = sheet.createRow(0)
        listOf(
            ExcelGermanLabels.Phases.NAME,
            ExcelGermanLabels.Phases.ABBREVIATION,
            ExcelGermanLabels.Phases.DURATION_WEEKS,
            ExcelGermanLabels.Phases.EFFORT_PT,
            ExcelGermanLabels.Phases.DEVELOPMENT_COST,
            ExcelGermanLabels.Phases.ADDITIONAL_COST_ONE_TIME,
            ExcelGermanLabels.Phases.ADDITIONAL_COST_RECURRING,
            ExcelGermanLabels.Phases.OFFER_PRICE_WITH_SALES_SURCHARGE
        ).forEachIndexed { idx, h -> headerRow.createCell(idx).setCellValue(h) }

        val salesSurcharge = version.parameterValue("salesSurcharge") ?: 0.1
        val dailyRate = version.parameterValue("dailyRate") ?: 800.0
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
        val sheet = workbook.createSheet(ExcelGermanLabels.Sheets.PARAMETERS)
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue(ExcelGermanLabels.Parameters.NAME)
        headerRow.createCell(1).setCellValue(ExcelGermanLabels.Parameters.VALUE)
        headerRow.createCell(2).setCellValue(ExcelGermanLabels.Parameters.COMMENT)

        version.parameters.forEachIndexed { idx, param ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(param.name)
            row.createCell(1).setCellValue(param.value)
            param.comment?.let { row.createCell(2).setCellValue(it) }
        }
    }

    private fun writeEffortDrivers(workbook: XSSFWorkbook, version: SubmittedEstimationVersion) {
        val sheet = workbook.createSheet(ExcelGermanLabels.Sheets.EFFORT_DRIVERS)
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue(ExcelGermanLabels.EffortDrivers.DESCRIPTION)
        headerRow.createCell(1).setCellValue(ExcelGermanLabels.EffortDrivers.FACTOR)
        headerRow.createCell(2).setCellValue(ExcelGermanLabels.EffortDrivers.ADDITIONAL_EFFORT)
        headerRow.createCell(3).setCellValue(ExcelGermanLabels.EffortDrivers.COMMENT)

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
