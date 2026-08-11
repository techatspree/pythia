package io.pythia.method

import io.pythia.model.CalculationParameters
import io.pythia.model.EstimationItem

/**
 * Service Provider Interface for a pluggable estimation method. Each method
 * lives in its own package under `io.pythia.method.<method>` and
 * registers a single module with the [EstimationMethodRegistry]. All
 * method-specific logic — the leaf shape, the per-leaf calculation into the
 * shared neutral values, and the export data-shaping — lives behind this
 * interface; the backend and frontend delegate to it.
 */
interface EstimationMethodModule {
    /** The method this module implements. */
    val method: EstimationMethod

    /**
     * Human-readable English description of the method, shown in the UI when the
     * user clicks the method badge (task-119). Single source of truth: the
     * backend serialises this verbatim on `GET /api/estimations/{id}` as
     * `methodDescription`; the frontend renders it verbatim.
     */
    val description: String

    /**
     * Apply [params] to a single leaf produced by this module, returning the
     * leaf with the shared neutral values (`mean`, `variance`, `offerPT`,
     * `cost`, `offerPrice`) populated — mirrors
     * [EstimationItem.withCalculationParameters].
     */
    fun calculate(item: EstimationItem, params: CalculationParameters): EstimationItem

    /**
     * Batch variant of [calculate] over all of a version's leaves at once. The
     * default just maps [calculate] per leaf; bucket methods override it because
     * they need per-bucket batch context (a non-sample leaf's value is derived
     * from the average of its bucket's sample siblings, invisible one leaf at a
     * time). [EstimationVersion.calculate] dispatches through this hook.
     */
    fun calculateAll(items: List<EstimationItem>, params: CalculationParameters): List<EstimationItem> =
        items.map { calculate(it, params) }

    /** Method-specific export cells for one leaf, appended after the shared columns. */
    fun exportRow(item: EstimationItem): List<String>

    /** Column headers matching the cells returned by [exportRow]. */
    fun exportColumnHeaders(): List<String>
}
