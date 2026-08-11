@file:OptIn(ExperimentalJsExport::class)

package io.pythia.model

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@JsExport
@DomainEntity
data class EstimationGroup(
    val title: String,
    val children: List<EstimationNode> = emptyList(),
    private val _logicalId: String = newId(),
    private val _id: String? = null,
    private val _createdAt: String? = null,
    private val _updatedAt: String? = null
) : EstimationNode(_logicalId, _id, _createdAt, _updatedAt) {

    override val mean: Double            get() = children.sumOf { it.mean }
    override val variance: Double        get() = children.sumOf { it.variance }
    override val riskSurcharge: Double   get() = children.sumOf { it.riskSurcharge }
    override val driverSurcharge: Double get() = children.sumOf { it.driverSurcharge }
    override val offerPT: Double         get() = children.sumOf { it.offerPT }
    override val cost: Double            get() = children.sumOf { it.cost }
    override val offerPrice: Double      get() = children.sumOf { it.offerPrice }

    override fun withCalculationParameters(params: CalculationParameters): EstimationGroup =
        copy(children = children.map { it.withCalculationParameters(params) })
}
