@file:OptIn(ExperimentalJsExport::class)

package io.pythia.model

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@JsExport
@DomainEntity
data class AdditionalCost(
    val description: String,
    val amount: Double = 0.0,
    val type: AdditionalCostType = AdditionalCostType.ONE_TIME,
    val amountPerWeek: Double = 0.0,
    val phase: ProjectPhase? = null,
    private val _id: String? = null,
    private val _createdAt: String? = null,
    private val _updatedAt: String? = null
) : BaseDomain(_id, _createdAt, _updatedAt)
