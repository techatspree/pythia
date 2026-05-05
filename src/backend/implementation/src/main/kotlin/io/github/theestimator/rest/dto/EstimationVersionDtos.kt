package io.github.theestimator.rest.dto

import io.github.theestimator.domain.*
import java.time.Instant
import java.util.UUID

data class EstimationVersionSummaryDto(
    val id: UUID?,
    val versionNumber: Int?,
    val status: EstimationVersionStatus,
    val totalEffort: Double?,
    val notes: String?,
    val createdAt: Instant?
)

data class EstimationVersionDto(
    val id: UUID?,
    val versionNumber: Int?,
    val status: EstimationVersionStatus,
    val totalEffort: Double?,
    val notes: String?,
    val createdAt: Instant?,
    val parameters: List<EstimationParameterDto>,
    val effortDrivers: List<EffortDriverDto>,
    val phases: List<ProjectPhaseDto>,
    val itemGroups: List<EstimationItemGroupDto>,
    val additionalCosts: List<AdditionalCostDto>
)

data class EstimationParameterDto(
    val id: UUID?,
    val name: String?,
    val value: Double,
    val comment: String?
)

data class EffortDriverDto(
    val id: UUID?,
    val description: String?,
    val factor: Double,
    val comment: String?
)

data class ProjectPhaseDto(
    val id: UUID?,
    val name: String?,
    val abbreviation: String?,
    val durationWeeks: Double?
)

data class EstimationItemGroupDto(
    val id: UUID?,
    val title: String?,
    val phaseAbbreviation: String?,
    val items: List<EstimationItemDto>
)

data class EstimationItemDto(
    val id: UUID?,
    val type: String,
    val description: String?,
    val code: String?,
    val minEffort: Double?,
    val expectedEffort: Double?,
    val maxEffort: Double?,
    val assumptions: String?,
    val mean: Double?,
    val variance: Double?,
    val riskSurcharge: Double?,
    val driverSurcharge: Double?,
    val offerPT: Double?,
    val cost: Double?,
    val offerPrice: Double?,
    val unit: String?
)

data class AdditionalCostDto(
    val id: UUID?,
    val description: String?,
    val amount: Double,
    val type: AdditionalCostType,
    val amountPerWeek: Double?,
    val phaseAbbreviation: String?
)

data class EstimationVersionUpdateDto(
    val notes: String? = null,
    val parameters: List<EstimationParameterDto>? = null,
    val effortDrivers: List<EffortDriverDto>? = null
)
