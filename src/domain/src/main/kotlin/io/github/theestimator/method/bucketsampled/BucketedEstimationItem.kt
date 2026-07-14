@file:OptIn(ExperimentalJsExport::class)

package io.github.theestimator.method.bucketsampled

import io.github.theestimator.model.CalculationParameters
import io.github.theestimator.model.DomainEntity
import io.github.theestimator.model.EstimationItem
import io.github.theestimator.model.PertCalculation
import io.github.theestimator.model.ProjectPhase
import io.github.theestimator.model.newId
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/**
 * Leaf item of the bucket + sampled three-point method. A leaf belongs to a
 * bucket (`bucketId`) and is either a **sample** — carrying its own three-point
 * `optimistic`/`likely`/`pessimistic` estimate — or a **non-sample**, whose
 * neutral values (`mean`, `variance`, `offerPT`) are the average of its bucket's
 * samples, assigned by [BucketMethodModule.calculateAll] via
 * [withCalculatedFromBucket] (a single item can't see its bucket's siblings).
 * `cost` / `offerPrice` derive from `offerPT` through the inherited getters.
 */
@Suppress("LongParameterList")
@JsExport
@DomainEntity
data class BucketedEstimationItem(
    val bucketId: String,
    val isSample: Boolean = false,
    val optimistic: Double? = null,
    val likely: Double? = null,
    val pessimistic: Double? = null,
    private val _description: String = "",
    private val _logicalId: String = newId(),
    private val _phase: ProjectPhase? = null,
    private val _calculationParameters: CalculationParameters = CalculationParameters(),
    // Bucket-derived values assigned to non-sample rows by the reducer:
    private val _bucketMean: Double? = null,
    private val _bucketVariance: Double? = null,
    private val _bucketOfferPT: Double? = null,
    private val _id: String? = null,
    private val _createdAt: String? = null,
    private val _updatedAt: String? = null
) : EstimationItem(
    _description,
    "",
    0.0,
    0.0,
    0.0,
    "",
    _phase,
    _logicalId,
    _calculationParameters,
    _id,
    _createdAt,
    _updatedAt
) {

    // Do NOT reference the parent's computed getters via a super-property here:
    // on Kotlin/JS that dispatches back to the override, causing infinite
    // recursion (see the reference comment in TimeRelativeEstimationItem). Inline
    // the PERT formula for sample rows; non-sample rows return the bucket-average
    // values stored by withCalculatedFromBucket.
    override val mean: Double
        get() = if (isSample) {
            PertCalculation.mean(optimistic ?: 0.0, likely ?: 0.0, pessimistic ?: 0.0)
        } else {
            _bucketMean ?: 0.0
        }

    override val variance: Double
        get() = if (isSample) {
            PertCalculation.variance(optimistic ?: 0.0, pessimistic ?: 0.0)
        } else {
            _bucketVariance ?: 0.0
        }

    override val offerPT: Double
        get() = if (isSample) {
            mean + mean * calculationParameters.riskFactor + mean * calculationParameters.totalDriverFactor
        } else {
            _bucketOfferPT ?: 0.0
        }

    override fun withCalculationParameters(params: CalculationParameters): BucketedEstimationItem =
        copy(_calculationParameters = params)

    /** Assign this bucket's per-sample-averaged neutral values (non-sample rows). */
    fun withCalculatedFromBucket(mean: Double, variance: Double, offerPT: Double): BucketedEstimationItem =
        copy(_bucketMean = mean, _bucketVariance = variance, _bucketOfferPT = offerPT)
}
