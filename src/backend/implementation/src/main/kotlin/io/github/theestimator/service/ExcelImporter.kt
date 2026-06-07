package io.github.theestimator.service

import io.github.theestimator.domain.AdditionalCostType
import io.github.theestimator.domain.Estimation
import io.github.theestimator.domain.draft.*
import jakarta.enterprise.context.ApplicationScoped
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream

@ApplicationScoped
class ExcelImporter {

    fun import(input: InputStream, estimation: Estimation, versionNumber: Int): DraftEstimationVersion {
        val workbook = XSSFWorkbook(input)
        val version = DraftEstimationVersion().apply {
            this.estimation = estimation
            this.versionNumber = versionNumber
        }

        importParameters(workbook.getSheet("Parameter"), version)
        importEffortDrivers(workbook.getSheet("Aufwandstreiber"), version)
        importPhases(workbook.getSheet("Pakete"), version)
        importEstimationItems(workbook.getSheet("Projektstrukturplan"), version)
        importAdditionalCosts(workbook.getSheet("Zusatzkosten"), version)

        workbook.close()
        return version
    }

    private fun importParameters(sheet: Sheet, version: DraftEstimationVersion) {
        for (rowIdx in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIdx) ?: continue
            val name = row.cellStringValue(0) ?: continue
            val value = row.cellNumericValue(1) ?: continue

            version.parameters.add(DraftEstimationParameter().apply {
                this.name = name
                this.value = value
                this.comment = row.cellStringValue(2)
                this.version = version
            })
        }
    }

    private fun importEffortDrivers(sheet: Sheet, version: DraftEstimationVersion) {
        for (rowIdx in 2..sheet.lastRowNum) {
            val row = sheet.getRow(rowIdx) ?: continue
            val description = row.cellStringValue(1) ?: continue
            val factor = row.cellNumericValue(2) ?: continue

            if (description == "Zusammenfassung der Aufwandstreiber") break

            version.effortDrivers.add(DraftEffortDriver().apply {
                this.description = description
                this.factor = factor
                this.comment = row.cellStringValue(4)
                this.version = version
            })
        }
    }

    private fun importPhases(sheet: Sheet, version: DraftEstimationVersion) {
        for (rowIdx in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIdx) ?: continue
            val name = row.cellStringValue(0) ?: continue
            val abbreviation = row.cellStringValue(1) ?: continue

            if (name == "Summe") break

            version.phases.add(DraftProjectPhase().apply {
                this.name = name
                this.abbreviation = abbreviation
                this.durationWeeks = row.cellNumericValue(2)
                this.version = version
            })
        }
    }

    private fun importEstimationItems(sheet: Sheet, version: DraftEstimationVersion) {
        val headerRow = 3
        var currentGroup: DraftGroupNode? = null
        var rootPosition = 0
        var isTimeRelativeSection = false

        for (rowIdx in (headerRow + 1)..sheet.lastRowNum) {
            val row = sheet.getRow(rowIdx) ?: continue
            val description = row.cellStringValue(0) ?: continue

            if (description == "Zusammenfassung") break

            if (description == "Aufwände relativ zur Zeit") {
                isTimeRelativeSection = true
                continue
            }

            val min = row.cellNumericValue(1)
            val expected = row.cellNumericValue(2)
            val max = row.cellNumericValue(3)

            if (min == null && expected == null && max == null) {
                currentGroup = DraftGroupNode().apply {
                    this.title = description
                    this.version = version
                    this.position = rootPosition++
                }
                version.roots.add(currentGroup)
            } else if (currentGroup != null) {
                val phaseAbbr = row.cellStringValue(14)
                val itemPhase = phaseAbbr?.let { abbr ->
                    version.phases.find { it.abbreviation == abbr }
                }

                val item: DraftEstimationNode = if (isTimeRelativeSection) {
                    DraftTimeRelativeItemNode()
                } else {
                    DraftFixedItemNode()
                }

                item.apply {
                    this.description = description
                    this.minEffort = min
                    this.expectedEffort = expected
                    this.maxEffort = max
                    this.assumptions = row.cellStringValue(15)
                    this.phase = itemPhase
                    this.version = version
                    this.parent = currentGroup
                    this.position = currentGroup.children.size
                }
                currentGroup.children.add(item)
            }
        }
    }

    private fun importAdditionalCosts(sheet: Sheet, version: DraftEstimationVersion) {
        var currentType = AdditionalCostType.ONE_TIME

        for (rowIdx in 0..sheet.lastRowNum) {
            val row = sheet.getRow(rowIdx) ?: continue
            val firstCell = row.cellStringValue(0) ?: continue

            when {
                firstCell == "Einmalige Kosten" -> currentType = AdditionalCostType.ONE_TIME
                firstCell == "Laufende Kosten" -> currentType = AdditionalCostType.RECURRING
                firstCell == "Beschreibung" -> continue
                else -> {
                    val amount = row.cellNumericValue(1) ?: continue
                    val phaseAbbr = row.cellStringValue(2)
                    val phase = phaseAbbr?.let { abbr ->
                        version.phases.find { it.abbreviation == abbr }
                    }

                    version.additionalCosts.add(DraftAdditionalCost().apply {
                        this.description = firstCell
                        this.amount = amount
                        this.type = currentType
                        this.amountPerWeek = if (currentType == AdditionalCostType.RECURRING) amount else null
                        this.phase = phase
                        this.version = version
                    })
                }
            }
        }
    }

    private fun Row.cellStringValue(colIdx: Int): String? {
        val cell = getCell(colIdx) ?: return null
        return try {
            cell.stringCellValue.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private fun Row.cellNumericValue(colIdx: Int): Double? {
        val cell = getCell(colIdx) ?: return null
        return try {
            cell.numericCellValue.takeIf { !it.isNaN() }
        } catch (e: Exception) {
            null
        }
    }
}
