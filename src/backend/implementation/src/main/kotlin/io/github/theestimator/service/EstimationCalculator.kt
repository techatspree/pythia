package io.github.theestimator.service

import io.github.theestimator.domain.*
import jakarta.enterprise.context.ApplicationScoped
import kotlin.math.sqrt

@ApplicationScoped
class EstimationCalculator {

    fun calculate(version: EstimationVersion) {
        val stdDevFactor = version.parameterValue("Standardabweichungsfaktor") ?: 2.0
        val dailyRate = version.parameterValue("Tagessatz") ?: 800.0
        val salesSurcharge = version.parameterValue("Vertriebszuschlag") ?: 0.1
        val totalDriverFactor = version.effortDrivers.sumOf { it.factor }

        val allItems = version.itemGroups.flatMap { it.items }

        // Calculate per-item values
        allItems.forEach { item ->
            val min = item.minEffort ?: 0.0
            val expected = item.expectedEffort ?: 0.0
            val max = item.maxEffort ?: 0.0

            item.mean = pertMean(min, expected, max)
            item.variance = pertVariance(min, max)
        }

        // Calculate risk surcharge based on total standard deviation
        val totalVariance = allItems.sumOf { it.variance ?: 0.0 }
        val stdDev = sqrt(totalVariance)
        val totalMean = allItems.sumOf { it.mean ?: 0.0 }
        val riskFactor = if (totalMean > 0) (stdDev * stdDevFactor) / totalMean else 0.0

        allItems.forEach { item ->
            val mean = item.mean ?: 0.0
            item.riskSurcharge = mean * riskFactor
            item.driverSurcharge = mean * totalDriverFactor
            item.offerPT = mean + (item.riskSurcharge ?: 0.0) + (item.driverSurcharge ?: 0.0)
            item.cost = (item.offerPT ?: 0.0) * dailyRate
            item.offerPrice = (item.cost ?: 0.0) * (1 + salesSurcharge)
        }

        version.totalEffort = allItems.sumOf { it.offerPT ?: 0.0 }
    }

    fun pertMean(min: Double, expected: Double, max: Double): Double =
        (min + 4 * expected + max) / 6.0

    fun pertVariance(min: Double, max: Double): Double {
        val range = (max - min) / 6.0
        return range * range
    }

    fun validateInvariants(version: EstimationVersion): List<InvariantResult> {
        val results = mutableListOf<InvariantResult>()
        val allItems = version.itemGroups.flatMap { it.items }
        val tolerance = 0.2

        // 1. Total offer PT == sum of all item offer PTs
        val totalOfferPT = allItems.sumOf { it.offerPT ?: 0.0 }
        results.add(InvariantResult(
            "Gesamtaufwand = Summe aller AngebotsPT",
            (version.totalEffort ?: 0.0) - totalOfferPT,
            tolerance
        ))

        // 2. Sum with risk in PSP == sum from calculation
        val totalMean = allItems.sumOf { it.mean ?: 0.0 }
        val totalVariance = allItems.sumOf { it.variance ?: 0.0 }
        val stdDevFactor = version.parameterValue("Standardabweichungsfaktor") ?: 2.0
        val totalDriverFactor = version.effortDrivers.sumOf { it.factor }
        val calculatedTotal = totalMean + sqrt(totalVariance) * stdDevFactor + totalMean * totalDriverFactor
        results.add(InvariantResult(
            "Summe mit Risiko im PSP = Summe bei Berechnung",
            totalOfferPT - calculatedTotal,
            tolerance
        ))

        // 3. Sum over packages == sum over subtotals
        val sumByGroups = version.itemGroups.sumOf { group ->
            group.items.sumOf { it.offerPT ?: 0.0 }
        }
        results.add(InvariantResult(
            "Summe über Arbeitspakete = Summe über Teilsummen",
            totalOfferPT - sumByGroups,
            tolerance
        ))

        // 4. Offer PT == offer PT per group
        val offerPTPerGroup = version.itemGroups.sumOf { group ->
            group.items.sumOf { it.offerPT ?: 0.0 }
        }
        results.add(InvariantResult(
            "Summe Angebots PT = Summe Angebots PT pro Gruppe",
            totalOfferPT - offerPTPerGroup,
            tolerance
        ))

        // 5. Cost in PSP == cost in package overview
        val totalCost = allItems.sumOf { it.cost ?: 0.0 }
        val dailyRate = version.parameterValue("Tagessatz") ?: 800.0
        val costFromEffort = totalOfferPT * dailyRate
        results.add(InvariantResult(
            "Kosten im PSP = Kosten in der Paketübersicht",
            totalCost - costFromEffort,
            tolerance
        ))

        // 6. Sum of variances == sum of group variances
        val varianceTotal = allItems.sumOf { it.variance ?: 0.0 }
        val varianceByGroups = version.itemGroups.sumOf { group ->
            group.items.sumOf { it.variance ?: 0.0 }
        }
        results.add(InvariantResult(
            "Summe der Varianzen = Summe der Varianzen der Gruppen",
            varianceTotal - varianceByGroups,
            tolerance
        ))

        return results
    }

    private fun EstimationVersion.parameterValue(name: String): Double? =
        parameters.find { it.name == name }?.value
}

data class InvariantResult(
    val description: String,
    val difference: Double,
    val tolerance: Double
) {
    val passed: Boolean get() = kotlin.math.abs(difference) <= tolerance
}
