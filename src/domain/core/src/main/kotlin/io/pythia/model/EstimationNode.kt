@file:OptIn(ExperimentalJsExport::class)

package io.pythia.model

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@JsExport
@DomainEntity
sealed class EstimationNode(
    val logicalId: String,
    id: String? = null,
    createdAt: String? = null,
    updatedAt: String? = null
) : BaseDomain(id, createdAt, updatedAt) {
    abstract val mean: Double
    abstract val variance: Double
    abstract val riskSurcharge: Double
    abstract val driverSurcharge: Double
    abstract val offerPT: Double
    abstract val cost: Double
    abstract val offerPrice: Double
    abstract fun withCalculationParameters(params: CalculationParameters): EstimationNode
}

fun EstimationNode.leaves(): Sequence<EstimationItem> = when (this) {
    is EstimationItem  -> sequenceOf(this)
    is EstimationGroup -> children.asSequence().flatMap { it.leaves() }
}
