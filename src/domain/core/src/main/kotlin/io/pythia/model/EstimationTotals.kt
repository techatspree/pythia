@file:OptIn(ExperimentalJsExport::class)

package io.pythia.model

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

private val logger = KotlinLogging.logger {}

/**
 * Every summed figure of one estimation version: the effort roll-up in person
 * days, the money roll-up, and the counts a UI needs to explain them.
 *
 * The money fields are only meaningful on a version whose leaves carry their
 * [CalculationParameters] — see [EstimationVersion.totals].
 */
@JsExport
data class EstimationTotals(
    val leafCount: Int,
    val meanPT: Double,
    val riskSurchargePT: Double,
    val driverSurchargePT: Double,
    val offerPT: Double,
    val developmentCost: Double,
    /**
     * The estimation tree's own offer price — [developmentCost] plus the sales
     * surcharge, EXCLUDING additional costs. This is what a grid column of
     * per-leaf offer prices sums to, so a "total" row over that column must use
     * this and not [totalOfferPrice].
     */
    val developmentOfferPrice: Double,
    val additionalOneTime: Double,
    val additionalRecurring: Double,
    val salesSurchargeAmount: Double,
    val totalOfferPrice: Double,
    /**
     * How many RECURRING additional costs have no phase and therefore
     * contribute nothing to [additionalRecurring]. Surfaced so the omission is
     * visible instead of silently lowering the price.
     */
    val recurringWithoutPhase: Int
)

/**
 * Reduces [version] to its [EstimationTotals]. The total offer price follows
 * the same formula the Excel export applies per phase, summed over the whole
 * estimate:
 *
 *     (developmentCost + additionalOneTime + additionalRecurring) * (1 + salesSurcharge)
 *
 * A RECURRING cost without a phase contributes 0 and is counted in
 * [EstimationTotals.recurringWithoutPhase]; a phase with a zero duration is a
 * legitimate 0 and is not counted.
 */
internal fun computeTotals(version: EstimationVersion): EstimationTotals {
    val leaves = version.roots.flatMap { it.leaves().toList() }
    val recurring = version.additionalCosts.filter { it.type == AdditionalCostType.RECURRING }

    val developmentCost = version.roots.sumOf { it.cost }
    val additionalOneTime = version.additionalCosts
        .filter { it.type == AdditionalCostType.ONE_TIME }
        .sumOf { it.amount }
    val additionalRecurring = recurring.sumOf { it.amountPerWeek * (it.phase?.durationWeeks ?: 0.0) }

    val netTotal = developmentCost + additionalOneTime + additionalRecurring
    val totalOfferPrice = netTotal * (1.0 + version.salesSurcharge)

    val totals = EstimationTotals(
        leafCount = leaves.size,
        meanPT = leaves.sumOf { it.mean },
        riskSurchargePT = leaves.sumOf { it.riskSurcharge },
        driverSurchargePT = leaves.sumOf { it.driverSurcharge },
        // Already computed by calculate(); reading it keeps the two from drifting.
        offerPT = version.totalEffort,
        developmentCost = developmentCost,
        developmentOfferPrice = version.roots.sumOf { it.offerPrice },
        additionalOneTime = additionalOneTime,
        additionalRecurring = additionalRecurring,
        salesSurchargeAmount = totalOfferPrice - netTotal,
        totalOfferPrice = totalOfferPrice,
        recurringWithoutPhase = recurring.count { it.phase == null }
    )

    logger.debug { "totals(): ${totals.leafCount} leaves, totalOfferPrice=${totals.totalOfferPrice}" }
    return totals
}
