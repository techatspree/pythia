package io.github.theestimator.rest.dto

import io.github.theestimator.domain.AdditionalCostType
import org.eclipse.microprofile.openapi.annotations.media.Schema
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

data class EstimationNodeDto(
    val logicalId: UUID?,
    @field:Schema(enumeration = ["GROUP", "FIXED", "TIME_RELATIVE", "BUCKETED"])
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
    val children: List<EstimationNodeDto>,
    // Reserved for the bucket+sampled method (task-100); populated once
    // task-102/103 land the BUCKETED persistence path.
    val bucketId: String? = null,
    val isSample: Boolean = false
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
    // Applied before roots so bucketed leaves can resolve their bucket
    // (bucket + sampled method only; null/empty for PERT).
    val buckets: List<BucketUpdateDto>? = null,
    val roots: List<EstimationNodeUpdateDto>? = null,
    val additionalCosts: List<AdditionalCostUpdateDto>? = null
)

data class BucketUpdateDto(
    val id: UUID? = null,
    val position: Int,
    val label: String
)

data class EstimationNodeUpdateDto(
    val logicalId: UUID? = null,
    @field:Schema(enumeration = ["GROUP", "FIXED", "TIME_RELATIVE", "BUCKETED"])
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
    val children: List<EstimationNodeUpdateDto> = emptyList(),
    // Reserved for the bucket+sampled method (task-100); currently rejected by
    // DraftUpdateApplier pending task-103. Sample rows reuse
    // minEffort/expectedEffort/maxEffort for their three-point values.
    val bucketId: String? = null,
    val isSample: Boolean = false
)
