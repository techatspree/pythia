package io.github.theestimator.rest.dto

import java.util.UUID

data class VersionComparisonDto(
    val versionA: Int,
    val versionB: Int,
    val addedItems: List<ComparisonItemDto>,
    val removedItems: List<ComparisonItemDto>,
    val modifiedItems: List<ItemModificationDto>,
    val addedGroups: List<ComparisonGroupDto>,
    val removedGroups: List<ComparisonGroupDto>,
    val parameterChanges: List<ParameterChangeDto>
)

data class ComparisonItemDto(
    val logicalId: UUID,
    val description: String?,
    val minEffort: Double?,
    val expectedEffort: Double?,
    val maxEffort: Double?,
    val offerPT: Double?,
    val groupTitle: String?
)

data class ComparisonGroupDto(
    val logicalId: UUID,
    val title: String?
)

data class ItemModificationDto(
    val logicalId: UUID,
    val description: String?,
    val before: ComparisonItemDto,
    val after: ComparisonItemDto,
    val changedFields: List<String>
)

data class ParameterChangeDto(
    val name: String,
    val oldValue: Double?,
    val newValue: Double?,
    val changeType: String
)
