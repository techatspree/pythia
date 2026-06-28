@file:OptIn(ExperimentalJsExport::class)

package io.github.theestimator.service

import io.github.theestimator.model.EstimationDefaults
import io.github.theestimator.model.EstimationVersion
import io.github.theestimator.model.leaves
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.math.abs
import kotlin.math.sqrt

@JsExport
open class EstimationCalculator {

    fun calculate(version: EstimationVersion): EstimationVersion =
        version.calculate()

    fun validateInvariants(version: EstimationVersion): Array<InvariantResult> {
        val results = mutableListOf<InvariantResult>()
        val allItems = version.roots.flatMap { it.leaves().toList() }
        val tolerance = INVARIANT_TOLERANCE

        val totalOfferPT = allItems.sumOf { it.offerPT }
        results.add(InvariantResult(
            "Gesamtaufwand = Summe aller AngebotsPT",
            version.totalEffort - totalOfferPT,
            tolerance
        ))

        val totalMean = allItems.sumOf { it.mean }
        val totalVariance = allItems.sumOf { it.variance }
        val stdDevFactor = version.parameterValue("Standardabweichungsfaktor") ?: EstimationDefaults.STD_DEV_FACTOR
        val totalDriverFactor = version.effortDrivers.sumOf { it.factor }
        val calculatedTotal = totalMean + sqrt(totalVariance) * stdDevFactor + totalMean * totalDriverFactor
        results.add(InvariantResult(
            "Summe mit Risiko im PSP = Summe bei Berechnung",
            totalOfferPT - calculatedTotal,
            tolerance
        ))

        val sumByRoots = version.roots.sumOf { it.offerPT }
        results.add(InvariantResult(
            "Summe der Wurzeln = Summe der Blätter (Akkumulation konsistent)",
            sumByRoots - totalOfferPT,
            tolerance
        ))

        val totalCost = allItems.sumOf { it.cost }
        val dailyRate = version.parameterValue("Tagessatz") ?: EstimationDefaults.DAILY_RATE
        val costFromEffort = totalOfferPT * dailyRate
        results.add(InvariantResult(
            "Kosten im PSP = Kosten in der Paketübersicht",
            totalCost - costFromEffort,
            tolerance
        ))

        val varianceByRoots = version.roots.sumOf { it.variance }
        results.add(InvariantResult(
            "Varianzakkumulation an der Wurzel = Summe der Blätter-Varianzen",
            varianceByRoots - totalVariance,
            tolerance
        ))

        return results.toTypedArray()
    }

    private companion object {
        /** Allowed rounding slack when checking accumulation invariants (PT). */
        const val INVARIANT_TOLERANCE = 0.2
    }
}

@JsExport
data class InvariantResult(
    val description: String,
    val difference: Double,
    val tolerance: Double
) {
    val passed: Boolean get() = abs(difference) <= tolerance
}
