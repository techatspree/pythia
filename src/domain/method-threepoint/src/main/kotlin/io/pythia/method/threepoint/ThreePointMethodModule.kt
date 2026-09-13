package io.pythia.method.threepoint

import io.pythia.method.EstimationMethod
import io.pythia.method.EstimationMethodModule
import io.pythia.model.CalculationParameters
import io.pythia.model.EstimationItem

/**
 * Three-point PERT estimation method (method #1) behind the SPI. A thin wrapper
 * over the existing per-leaf PERT mapping ([EstimationItem.withCalculationParameters])
 * and the PERT export column shape. This package also owns the PERT leaf types
 * `FixedEstimationItem` / `TimeRelativeEstimationItem` (task-113 opened
 * `EstimationItem` from `sealed` to `abstract` so each method owns its leaf).
 */
class ThreePointMethodModule : EstimationMethodModule {
    override val method: EstimationMethod = EstimationMethod.THREE_POINT_PERT

    override val description: String =
        "PERT three-point estimation: for each work item, capture " +
            "optimistic, most-likely, and pessimistic estimates. Mean and " +
            "variance follow the classic PERT formulas; group totals " +
            "accumulate from the leaves."

    override fun calculate(item: EstimationItem, params: CalculationParameters): EstimationItem =
        item.withCalculationParameters(params)

    // PERT method-specific input columns: the three-point estimates.
    override fun exportColumnHeaders(): List<String> = listOf("Min", "Expected", "Max")

    override fun exportRow(item: EstimationItem): List<String> = listOf(
        item.minEffort.toString(),
        item.expectedEffort.toString(),
        item.maxEffort.toString()
    )

    /**
     * Min/Expected/Max back into a leaf. A cell that is blank or unparseable
     * reads as 0.0 — the same value an empty three-point input carries — but a
     * row with the wrong number of cells is not a PERT row at all.
     */
    override fun importRow(cells: List<String>): EstimationItem? {
        if (cells.size != EXPORT_COLUMNS) return null
        return FixedEstimationItem(
            _description = "",
            _minEffort = cells[0].toDoubleOrNull() ?: 0.0,
            _expectedEffort = cells[1].toDoubleOrNull() ?: 0.0,
            _maxEffort = cells[2].toDoubleOrNull() ?: 0.0
        )
    }

    private companion object {
        const val EXPORT_COLUMNS = 3
    }
}
