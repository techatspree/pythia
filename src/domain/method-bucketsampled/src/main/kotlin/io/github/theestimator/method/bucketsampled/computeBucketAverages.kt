// Two cohesive top-level declarations (the DTO + its producer) intentionally
// share this file, named after the public function; the DTO is its result type.
@file:Suppress("MatchingDeclarationName")

package io.github.theestimator.method.bucketsampled

import io.github.theestimator.model.CalculationParameters

/** Per-bucket average of the sample items' neutral values. */
internal data class BucketAverages(
    val mean: Double,
    val variance: Double,
    val offerPT: Double
)

/**
 * Average the PERT-derived neutral values across each bucket's sample items.
 * Non-sample items in a bucket inherit these. Only samples contribute; a bucket
 * with no samples yields no entry. `offerPT` depends on [params] (risk/driver
 * surcharge), so the samples are calculated with [params] before averaging.
 */
internal fun computeBucketAverages(
    items: List<BucketedEstimationItem>,
    params: CalculationParameters
): Map<String, BucketAverages> =
    items.filter { it.isSample }
        .groupBy { it.bucketId }
        .mapValues { (_, samples) ->
            val calculated = samples.map { it.withCalculationParameters(params) }
            BucketAverages(
                mean = calculated.map { it.mean }.average(),
                variance = calculated.map { it.variance }.average(),
                offerPT = calculated.map { it.offerPT }.average()
            )
        }
