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
    val roots: List<EstimationNodeDto>,
    val additionalCosts: List<AdditionalCostDto>,
    // task-054 compat shim: a flattened depth-1 view of `roots` for the
    // unmodified frontend that still reads `itemGroups`. Removed when
    // task-054 rewires src/frontend/src/lib/api/types.ts and the version
    // detail page to read `roots` directly.
    val itemGroups: List<LegacyItemGroupDto>
)

@Deprecated("Compat shim for the unmodified frontend; removed in task-054")
data class LegacyItemGroupDto(
    val logicalId: UUID?,
    val title: String,
    val items: List<LegacyItemDto>
)

@Deprecated("Compat shim for the unmodified frontend; removed in task-054")
data class LegacyItemDto(
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
    val phaseAbbreviation: String?
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

data class EstimationNodeDto(
    val logicalId: UUID?,
    val type: String,
    val title: String?,
    val description: String?,
    val code: String?,
    val minEffort: Double?,
    val expectedEffort: Double?,
    val maxEffort: Double?,
    val assumptions: String?,
    val mean: Double,
    val variance: Double,
    val riskSurcharge: Double,
    val driverSurcharge: Double,
    val offerPT: Double,
    val cost: Double,
    val offerPrice: Double,
    val unit: String?,
    val phaseAbbreviation: String?,
    val children: List<EstimationNodeDto>
)

data class AdditionalCostDto(
    val id: UUID? = null,
    val description: String,
    val amount: Double,
    val type: AdditionalCostType,
    val amountPerWeek: Double?,
    val phaseAbbreviation: String?
)

data class AdditionalCostUpdateDto(
    val id: UUID? = null,
    val description: String,
    val amount: Double = 0.0,
    val type: AdditionalCostType,
    val amountPerWeek: Double? = null,
    val phaseAbbreviation: String? = null
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
    val roots: List<EstimationNodeUpdateDto>? = null,
    val additionalCosts: List<AdditionalCostUpdateDto>? = null,
    // task-054 compat shim: the unmodified frontend still sends `itemGroups`
    // in the autosave payload. When provided (and `roots` is null), the
    // server translates it into the canonical `roots` shape. Removed in
    // task-054.
    val itemGroups: List<LegacyItemGroupUpdateDto>? = null
)

@Deprecated("Compat shim for the unmodified frontend; removed in task-054")
data class LegacyItemGroupUpdateDto(
    val logicalId: UUID? = null,
    val title: String,
    val items: List<LegacyItemUpdateDto> = emptyList()
)

@Deprecated("Compat shim for the unmodified frontend; removed in task-054")
data class LegacyItemUpdateDto(
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

data class EstimationNodeUpdateDto(
    val logicalId: UUID? = null,
    val type: String,
    val title: String? = null,
    val description: String? = null,
    val code: String? = null,
    val minEffort: Double? = null,
    val expectedEffort: Double? = null,
    val maxEffort: Double? = null,
    val assumptions: String? = null,
    val unit: String? = null,
    val phaseAbbreviation: String? = null,
    val children: List<EstimationNodeUpdateDto> = emptyList()
)
