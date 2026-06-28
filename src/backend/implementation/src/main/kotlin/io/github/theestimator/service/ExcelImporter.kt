package io.github.theestimator.service

import io.github.theestimator.domain.AdditionalCostType
import io.github.theestimator.domain.Estimation
import io.github.theestimator.domain.draft.DraftAdditionalCost
import io.github.theestimator.domain.draft.DraftEffortDriver
import io.github.theestimator.domain.draft.DraftEstimationNode
import io.github.theestimator.domain.draft.DraftEstimationParameter
import io.github.theestimator.domain.draft.DraftEstimationVersion
import io.github.theestimator.domain.draft.DraftFixedItemNode
import io.github.theestimator.domain.draft.DraftGroupNode
import io.github.theestimator.domain.draft.DraftProjectPhase
import io.github.theestimator.domain.draft.DraftTimeRelativeItemNode
import jakarta.enterprise.context.ApplicationScoped
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.util.UUID

// The integer literals here are POI spreadsheet column indices — the physical
// layout of the imported sheet. Naming a constant per column would obscure the
// layout rather than clarify it, so MagicNumber is suppressed for this class.
@Suppress("MagicNumber")
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
        // The exporter writes one row per node in tree order. We read:
        //   col 0  — indented title/description
        //   col 1..3, 14, 15 — leaf effort/phase/assumptions (unchanged)
        //   col 16 — "Node type" (GROUP / FIXED / TIME_RELATIVE)
        //   col 17 — "Logical ID" (UUID, preserves identity across round-trips)
        //   col 18 — "Unit" (TIME_RELATIVE only)
        // POI's outline level encodes depth: a level-N row attaches to the
        // most recent row at level N-1.
        val lastAtLevel = mutableListOf<DraftEstimationNode>()

        for (rowIdx in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIdx) ?: continue
            val nodeType = row.cellStringValue(16) ?: continue
            val level = (row as XSSFRow).outlineLevel.toInt()
            val label = row.cellStringValue(0)?.trimStart() ?: ""

            val node: DraftEstimationNode = when (nodeType) {
                "GROUP" -> DraftGroupNode().apply { title = label }
                "TIME_RELATIVE" -> DraftTimeRelativeItemNode().apply {
                    description = label
                    unit = row.cellStringValue(18) ?: "h/Woche"
                }
                "FIXED" -> DraftFixedItemNode().apply { description = label }
                else -> continue
            }

            row.cellStringValue(17)?.let { node.logicalId = UUID.fromString(it) }

            if (nodeType != "GROUP") {
                node.minEffort = row.cellNumericValue(1)
                node.expectedEffort = row.cellNumericValue(2)
                node.maxEffort = row.cellNumericValue(3)
                node.assumptions = row.cellStringValue(15)
                row.cellStringValue(14)?.let { abbr ->
                    node.phase = version.phases.find { it.abbreviation == abbr }
                }
            }

            node.version = version

            if (level == 0) {
                node.parent = null
                node.position = version.roots.size
                version.roots.add(node)
            } else {
                val parent = lastAtLevel.getOrNull(level - 1)
                    ?: error("Row $rowIdx outline level $level has no ancestor at level ${level - 1}")
                node.parent = parent
                node.position = parent.children.size
                parent.children.add(node)
            }

            while (lastAtLevel.size <= level) lastAtLevel.add(node)
            lastAtLevel[level] = node
            // truncate deeper levels — they no longer apply once we move up
            while (lastAtLevel.size > level + 1) lastAtLevel.removeAt(lastAtLevel.size - 1)
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
            e.printStackTrace()
            null
        }
    }

    private fun Row.cellNumericValue(colIdx: Int): Double? {
        val cell = getCell(colIdx) ?: return null
        return try {
            cell.numericCellValue.takeIf { !it.isNaN() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
