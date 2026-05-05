package io.github.theestimator.service

import io.github.theestimator.model.*
import kotlin.math.sqrt

open class EstimationCalculator {

    fun calculate(version: EstimationVersion): EstimationVersion =
        version.calculate()

    fun validateInvariants(version: EstimationVersion): List<InvariantResult> {
        val results = mutableListOf<InvariantResult>()
        val allItems = version.itemGroups.flatMap { it.items }
        val tolerance = 0.2

        val totalOfferPT = allItems.sumOf { it.offerPT }
        results.add(InvariantResult(
            "Gesamtaufwand = Summe aller AngebotsPT",
            (version.totalEffort ?: 0.0) - totalOfferPT,
            tolerance
        ))

        val totalMean = allItems.sumOf { it.mean }
        val totalVariance = allItems.sumOf { it.variance }
        val stdDevFactor = version.parameterValue("Standardabweichungsfaktor") ?: 2.0
        val totalDriverFactor = version.effortDrivers.sumOf { it.factor }
        val calculatedTotal = totalMean + sqrt(totalVariance) * stdDevFactor + totalMean * totalDriverFactor
        results.add(InvariantResult(
            "Summe mit Risiko im PSP = Summe bei Berechnung",
            totalOfferPT - calculatedTotal,
            tolerance
        ))

        val sumByGroups = version.itemGroups.sumOf { group ->
            group.items.sumOf { it.offerPT }
        }
        results.add(InvariantResult(
            "Summe über Arbeitspakete = Summe über Teilsummen",
            totalOfferPT - sumByGroups,
            tolerance
        ))

        val offerPTPerGroup = version.itemGroups.sumOf { group ->
            group.items.sumOf { it.offerPT }
        }
        results.add(InvariantResult(
            "Summe Angebots PT = Summe Angebots PT pro Gruppe",
            totalOfferPT - offerPTPerGroup,
            tolerance
        ))

        val totalCost = allItems.sumOf { it.cost }
        val dailyRate = version.parameterValue("Tagessatz") ?: 800.0
        val costFromEffort = totalOfferPT * dailyRate
        results.add(InvariantResult(
            "Kosten im PSP = Kosten in der Paketübersicht",
            totalCost - costFromEffort,
            tolerance
        ))

        val varianceTotal = allItems.sumOf { it.variance }
        val varianceByGroups = version.itemGroups.sumOf { group ->
            group.items.sumOf { it.variance }
        }
        results.add(InvariantResult(
            "Summe der Varianzen = Summe der Varianzen der Gruppen",
            varianceTotal - varianceByGroups,
            tolerance
        ))

        return results
    }
}

data class InvariantResult(
    val description: String,
    val difference: Double,
    val tolerance: Double
) {
    val passed: Boolean get() = kotlin.math.abs(difference) <= tolerance
}
