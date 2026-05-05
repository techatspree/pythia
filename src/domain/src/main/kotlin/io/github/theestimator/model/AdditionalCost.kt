package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
data class AdditionalCost(
    val description: String,
    val amount: Double = 0.0,
    val type: AdditionalCostType = AdditionalCostType.ONE_TIME,
    val amountPerWeek: Double = 0.0,
    val phase: ProjectPhase? = null,
    private val _id: UUID? = null,
    private val _createdAt: Instant? = null,
    private val _updatedAt: Instant? = null
) : BaseDomain(_id, _createdAt, _updatedAt)
