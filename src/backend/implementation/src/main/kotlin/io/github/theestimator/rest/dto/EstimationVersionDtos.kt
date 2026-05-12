package io.github.theestimator.rest.dto

import io.github.theestimator.domain.AdditionalCostType
import java.time.Instant
import java.util.UUID

data class EstimationVersionSummaryDto(
    val versionNumber: Int,
    val isDraft: Boolean,
    val totalEffort: Double?,
    val notes: String?,
    val createdAt: Instant?
)

data class EstimationVersionDto(
    val versionNumber: Int,
    val isDraft: Boolean,
    val totalEffort: Double,
    val notes: String?,
    val createdAt: Instant?,
    val submittedAt: Instant?,
    val parameters: List<EstimationParameterDto>,
    val effortDrivers: List<EffortDriverDto>,
    val phases: List<ProjectPhaseDto>,
    val itemGroups: List<EstimationItemGroupDto>,
    val additionalCosts: List<AdditionalCostDto>
)

data class EstimationParameterDto(
    val id: UUID? = null,
    val name: String,
    val value: Double,
    val comment: String? = null
)

data class EffortDriverDto(
    val id: UUID? = null,
    val description: String,
    val factor: Double,
    val comment: String? = null
)

data class ProjectPhaseDto(
    val id: UUID? = null,
    val name: String,
    val abbreviation: String,
    val durationWeeks: Double? = null
)

data class EstimationItemGroupDto(
    val logicalId: UUID?,
    val title: String,
    val items: List<EstimationItemDto>
)

data class EstimationItemDto(
    val logicalId: UUID?,
    val type: String,
    val description: String,
    val code: String?,
    val minEffort: Double,
    val expectedEffort: Double,
    val maxEffort: Double,
    val assumptions: String?,
    val mean: Double,
    val variance: Double,
    val riskSurcharge: Double,
    val driverSurcharge: Double,
    val offerPT: Double,
    val cost: Double,
    val offerPrice: Double,
    val unit: String?,
    val phaseAbbreviation: String? = null
)

data class AdditionalCostDto(
    val id: UUID? = null,
    val description: String,
    val amount: Double,
    val type: AdditionalCostType,
    val amountPerWeek: Double?,
    val phaseAbbreviation: String?
)

data class PhaseUpdateDto(
    val name: String,
    val abbreviation: String,
    val durationWeeks: Double? = null
)

data class DraftUpdateDto(
    val notes: String? = null,
    val parameters: List<EstimationParameterDto>? = null,
    val effortDrivers: List<EffortDriverDto>? = null,
    val phases: List<PhaseUpdateDto>? = null,
    val itemGroups: List<ItemGroupUpdateDto>? = null
)

data class ItemGroupUpdateDto(
    val logicalId: UUID? = null,
    val title: String,
    val items: List<EstimationItemUpdateDto>
)

data class EstimationItemUpdateDto(
    val logicalId: UUID? = null,
    val description: String,
    val minEffort: Double? = null,
    val expectedEffort: Double? = null,
    val maxEffort: Double? = null,
    val assumptions: String? = null,
    val phaseAbbreviation: String? = null,
    val type: String = "FIXED",
    val unit: String? = null
)
