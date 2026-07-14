package io.github.theestimator.method.bucketsampled

import io.github.theestimator.method.EstimationMethod
import io.github.theestimator.method.EstimationMethodModule
import io.github.theestimator.model.CalculationParameters
import io.github.theestimator.model.EstimationItem
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Bucket + sampled three-point estimation method (method #2) behind the SPI.
 * Owns the [BucketedEstimationItem] leaf. Overrides [calculateAll] because a
 * non-sample leaf's value is the average of its bucket's sample siblings, which
 * is only visible with the whole leaf list in hand (the per-leaf [calculate]
 * default cannot express it).
 */
class BucketMethodModule : EstimationMethodModule {

    init {
        logger.info { "Registered estimation method module: ${EstimationMethod.BUCKET_SAMPLED_PERT}" }
    }

    override val method: EstimationMethod = EstimationMethod.BUCKET_SAMPLED_PERT

    override fun calculate(item: EstimationItem, params: CalculationParameters): EstimationItem =
        item.withCalculationParameters(params)

    override fun calculateAll(items: List<EstimationItem>, params: CalculationParameters): List<EstimationItem> {
        val averages = computeBucketAverages(items.filterIsInstance<BucketedEstimationItem>(), params)
        return items.map { item ->
            val average = if (item is BucketedEstimationItem && !item.isSample) averages[item.bucketId] else null
            if (average != null && item is BucketedEstimationItem) {
                item.withCalculatedFromBucket(average.mean, average.variance, average.offerPT)
                    .withCalculationParameters(params)
            } else {
                item.withCalculationParameters(params)
            }
        }
    }

    // Bucket method-specific input columns: bucket id, sample flag, and the
    // three-point estimate (empty on non-sample rows).
    override fun exportColumnHeaders(): List<String> =
        listOf("Bucket", "Is Sample", "Optimistic", "Likely", "Pessimistic")

    override fun exportRow(item: EstimationItem): List<String> {
        val bucketed = item as? BucketedEstimationItem ?: return listOf("", "", "", "", "")
        return if (bucketed.isSample) {
            listOf(
                bucketed.bucketId,
                "true",
                bucketed.optimistic?.toString() ?: "",
                bucketed.likely?.toString() ?: "",
                bucketed.pessimistic?.toString() ?: ""
            )
        } else {
            listOf(bucketed.bucketId, "false", "", "", "")
        }
    }
}
