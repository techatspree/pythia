package io.pythia.service

import io.pythia.domain.AdditionalCostType
import io.pythia.domain.Estimation
import io.pythia.domain.draft.DraftAdditionalCost
import io.pythia.domain.draft.DraftEffortDriver
import io.pythia.domain.draft.DraftEstimationNode
import io.pythia.domain.EstimationBucket
import io.pythia.domain.draft.DraftBucketedItemNode
import io.pythia.domain.draft.DraftEstimationVersion
import io.pythia.method.EstimationMethod
import io.pythia.method.EstimationMethodModule
import io.pythia.method.EstimationMethodRegistry
import io.pythia.method.bucketsampled.BucketedEstimationItem
import io.pythia.model.EstimationItem
import io.pythia.domain.draft.DraftFixedItemNode
import io.pythia.domain.draft.DraftGroupNode
import io.pythia.domain.draft.DraftProjectPhase
import io.pythia.domain.draft.DraftTimeRelativeItemNode
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.util.UUID

// The integer literals here are POI spreadsheet column indices — the physical
// layout of the imported sheet (MagicNumber). Robust spreadsheet parsing is
// inherently branchy: row/cell scans use multiple break/continue
// (LoopWithTooManyJumpStatements), the import methods cover many optional
// columns (CyclomaticComplexMethod), and per-cell reads catch a broad
// Exception to fall back gracefully on malformed cells (TooGenericExceptionCaught).
@Suppress(
    "MagicNumber",
    "CyclomaticComplexMethod",
    "LoopWithTooManyJumpStatements",
    "TooGenericExceptionCaught"
)
@ApplicationScoped
class ExcelImporter {

    private companion object {
        /** Description sits at 0; the method's own cells start right after it. */
        const val METHOD_FIRST_COLUMN = 1

        /** The column count the fixed trailing indices were written for. */
        const val PERT_METHOD_COLUMNS = 3
    }

    fun import(input: InputStream, estimation: Estimation, versionNumber: Int): DraftEstimationVersion {
        Log.info("Importing Excel workbook into estimation ${estimation.id} version $versionNumber")
        val workbook = XSSFWorkbook(input)
        val version = DraftEstimationVersion().apply {
            this.estimation = estimation
            this.versionNumber = versionNumber
        }

        // The workbook's method-specific columns are shaped by the estimation's
        // own method (task-182), exactly as the exporter shaped them on the way
        // out — same module, same column count, so the two cannot drift.
        val module = EstimationMethodRegistry.require(estimation.method)
        Log.info("Import uses method ${estimation.method} for estimation ${estimation.id}")
        val bucketsByFileId = importBuckets(workbook.getSheet(ExcelGermanLabels.Sheets.BUCKETS), estimation)

        importParameters(workbook.getSheet(ExcelGermanLabels.Sheets.PARAMETERS), version)
        importEffortDrivers(workbook.getSheet(ExcelGermanLabels.Sheets.EFFORT_DRIVERS), version)
        importPhases(workbook.getSheet(ExcelGermanLabels.Sheets.PHASES), version)
        importEstimationItems(
            workbook.getSheet(ExcelGermanLabels.Sheets.PROJECT_STRUCTURE_PLAN),
            version,
            module,
            bucketsByFileId
        )
        importAdditionalCosts(workbook.getSheet(ExcelGermanLabels.Sheets.ADDITIONAL_COSTS), version)

        workbook.close()
        return version
    }

    /**
     * Reads the Eimer sheet and returns `fileBucketId -> bucket of THIS estimation`.
     *
     * Matching is by LABEL, never by id: a workbook's bucket ids belong to the
     * estimation that wrote it and mean nothing here. A label with no bucket yet
     * is created, so a file can be imported into an estimation that has none.
     */
    private fun importBuckets(sheet: Sheet?, estimation: Estimation): Map<String, EstimationBucket> {
        if (sheet == null) return emptyMap()
        val byFileId = mutableMapOf<String, EstimationBucket>()
        for (rowIdx in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIdx) ?: continue
            val label = row.cellStringValue(0) ?: continue
            val position = row.cellNumericValue(1)?.toInt() ?: rowIdx - 1
            val fileId = row.cellStringValue(2) ?: continue
            val bucket = estimation.buckets.firstOrNull { it.label == label }
                ?: EstimationBucket().apply {
                    this.label = label
                    this.position = position
                    this.estimation = estimation
                }.also { estimation.buckets.add(it) }
            byFileId[fileId] = bucket
        }
        Log.debug("Excel import resolved ${byFileId.size} bucket(s) by label")
        return byFileId
    }

    private fun importParameters(sheet: Sheet, version: DraftEstimationVersion) {
        for (rowIdx in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIdx) ?: continue
            val name = row.cellStringValue(0) ?: continue
            val value = row.cellNumericValue(1) ?: continue

            // The parameters are typed fields now (task-138), so map the sheet's
            // labels onto them instead of storing arbitrary rows. Accepts the
            // German labels this exporter writes; any other row is ignored
            // rather than silently becoming a parameter nothing reads.
            when (name.trim()) {
                ExcelGermanLabels.Parameters.DAILY_RATE -> version.dailyRate = value
                ExcelGermanLabels.Parameters.STD_DEV_FACTOR -> version.stdDevFactor = value
                ExcelGermanLabels.Parameters.SALES_SURCHARGE -> version.salesSurcharge = value
                else -> Log.debug("Ignoring unknown parameter row '\$name' in the imported workbook")
            }
        }
    }

    private fun importEffortDrivers(sheet: Sheet, version: DraftEstimationVersion) {
        for (rowIdx in 2..sheet.lastRowNum) {
            val row = sheet.getRow(rowIdx) ?: continue
            val description = row.cellStringValue(1) ?: continue
            val factor = row.cellNumericValue(2) ?: continue

            if (description == ExcelGermanLabels.EffortDrivers.SUMMARY_MARKER) break

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

            if (name == ExcelGermanLabels.Phases.TOTAL_MARKER) break

            version.phases.add(DraftProjectPhase().apply {
                this.name = name
                this.abbreviation = abbreviation
                this.durationWeeks = row.cellNumericValue(2)
                this.version = version
            })
        }
    }

    private fun importEstimationItems(
        sheet: Sheet,
        version: DraftEstimationVersion,
        module: EstimationMethodModule,
        bucketsByFileId: Map<String, EstimationBucket>
    ) {
        // Column count and therefore every trailing index follow the METHOD, the
        // same way ExcelExporter shifts them (task-107/182). Both sides read it
        // from exportColumnHeaders(), so they move together.
        val methodColumns = module.exportColumnHeaders().size
        val shift = methodColumns - PERT_METHOD_COLUMNS
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
            val nodeType = row.cellStringValue(16 + shift) ?: continue
            val level = (row as XSSFRow).outlineLevel
            val label = row.cellStringValue(0)?.trimStart() ?: ""

            val node: DraftEstimationNode = when (nodeType) {
                "GROUP" -> DraftGroupNode().apply { title = label }
                "TIME_RELATIVE" -> DraftTimeRelativeItemNode().apply {
                    description = label
                    unit = row.cellStringValue(18 + shift) ?: ExcelGermanLabels.HOURS_PER_WEEK_UNIT
                }
                "FIXED" -> leafNode(row, label, module, methodColumns, bucketsByFileId)
                    ?: continue
                else -> continue
            }

            row.cellStringValue(17 + shift)?.let { node.logicalId = UUID.fromString(it) }

            if (nodeType != "GROUP") {
                // The effort triple is set by leafNode() for a FIXED row, which
                // takes it from the method module rather than from fixed columns.
                if (nodeType == "TIME_RELATIVE") {
                    node.minEffort = row.cellNumericValue(1)
                    node.expectedEffort = row.cellNumericValue(2)
                    node.maxEffort = row.cellNumericValue(3)
                }
                node.assumptions = row.cellStringValue(15 + shift)
                row.cellStringValue(14 + shift)?.let { abbr ->
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
                firstCell == ExcelGermanLabels.AdditionalCosts.ONE_TIME_SECTION ->
                    currentType = AdditionalCostType.ONE_TIME
                firstCell == ExcelGermanLabels.AdditionalCosts.RECURRING_SECTION ->
                    currentType = AdditionalCostType.RECURRING
                firstCell == ExcelGermanLabels.AdditionalCosts.DESCRIPTION -> continue
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

    /**
     * Builds a leaf from the row's METHOD cells, which the module parses — the
     * importer never learns which column means what. The resulting draft node is
     * bucketed when the method produced a bucketed item, so a bucket workbook
     * round-trips instead of collapsing into plain fixed items.
     */
    private fun leafNode(
        row: Row,
        label: String,
        module: EstimationMethodModule,
        methodColumns: Int,
        bucketsByFileId: Map<String, EstimationBucket>
    ): DraftEstimationNode? {
        val cells = (0 until methodColumns).map { row.cellAsText(METHOD_FIRST_COLUMN + it) }
        val item: EstimationItem? = module.importRow(cells)
        if (item == null) {
            Log.warn("Skipping row '$label': its cells are not a valid row for ${module.method}")
            return null
        }
        return if (item is BucketedEstimationItem) {
            DraftBucketedItemNode().apply {
                description = label
                bucket = bucketsByFileId[item.bucketId]
                isSample = item.isSample
                minEffort = item.optimistic
                expectedEffort = item.likely
                maxEffort = item.pessimistic
            }
        } else {
            DraftFixedItemNode().apply {
                description = label
                minEffort = item.minEffort
                expectedEffort = item.expectedEffort
                maxEffort = item.maxEffort
            }
        }
    }
}

/** A cell as text whatever its type — the exporter writes numbers as NUMERIC. */
private fun Row.cellAsText(colIdx: Int): String =
    cellStringValue(colIdx) ?: cellNumericValue(colIdx)?.toString() ?: ""

private fun Row.cellStringValue(colIdx: Int): String? {
    val cell = getCell(colIdx) ?: return null
    return try {
        cell.stringCellValue.takeIf { it.isNotBlank() }
    } catch (e: IllegalStateException) {
        Log.debug("Failed to read cell value during Excel import", e)
        null
    }
}

private fun Row.cellNumericValue(colIdx: Int): Double? {
    val cell = getCell(colIdx) ?: return null
    return try {
        cell.numericCellValue.takeIf { !it.isNaN() }
    } catch (e: IllegalStateException) {
        Log.debug("Failed to read cell value during Excel import", e)
        null
    }
}
