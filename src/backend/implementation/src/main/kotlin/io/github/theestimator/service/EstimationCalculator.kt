package io.github.theestimator.service

import io.github.theestimator.domain.draft.DraftEstimationItem
import io.github.theestimator.domain.draft.DraftEstimationVersion
import io.github.theestimator.model.PertCalculation
import jakarta.enterprise.context.ApplicationScoped
import kotlin.math.sqrt

@ApplicationScoped
class EstimationCalculator {

    fun calculate(draft: DraftEstimationVersion): CalculationResult {
        val stdDevFactor = draft.parameterValue("Standardabweichungsfaktor") ?: 2.0
        val dailyRate = draft.parameterValue("Tagessatz") ?: 800.0
        val salesSurcharge = draft.parameterValue("Vertriebszuschlag") ?: 0.1
        val totalDriverFactor = draft.effortDrivers.sumOf { it.factor }

        val allItems = draft.itemGroups.flatMap { it.items }

        val itemResults = allItems.map { item ->
            val min = item.minEffort ?: 0.0
            val expected = item.expectedEffort ?: 0.0
            val max = item.maxEffort ?: 0.0
            val mean = PertCalculation.mean(min, expected, max)
            val variance = PertCalculation.variance(min, max)
            ItemMeanVariance(item, mean, variance)
        }

        val totalVariance = itemResults.sumOf { it.variance }
        val totalMean = itemResults.sumOf { it.mean }
        val riskFactor = PertCalculation.riskFactor(totalMean, totalVariance, stdDevFactor)

        val calculatedItems = itemResults.map { (item, mean, variance) ->
            val riskSurcharge = mean * riskFactor
            val driverSurcharge = mean * totalDriverFactor
            val offerPT = mean + riskSurcharge + driverSurcharge
            val cost = offerPT * dailyRate
            val offerPrice = cost * (1 + salesSurcharge)
            CalculatedItem(
                item = item,
                mean = mean,
                variance = variance,
                riskSurcharge = riskSurcharge,
                driverSurcharge = driverSurcharge,
                offerPT = offerPT,
                cost = cost,
                offerPrice = offerPrice
            )
        }

        val totalEffort = calculatedItems.sumOf { it.offerPT }

        return CalculationResult(
            totalEffort = totalEffort,
            items = calculatedItems
        )
    }
}

private data class ItemMeanVariance(
    val item: DraftEstimationItem,
    val mean: Double,
    val variance: Double
)

data class CalculationResult(
    val totalEffort: Double,
    val items: List<CalculatedItem>
)

data class CalculatedItem(
    val item: DraftEstimationItem,
    val mean: Double,
    val variance: Double,
    val riskSurcharge: Double,
    val driverSurcharge: Double,
    val offerPT: Double,
    val cost: Double,
    val offerPrice: Double
)
