package io.pythia.rest.dto

import java.util.UUID

data class VersionComparisonDto(
    val versionA: Int,
    val versionB: Int,
    val addedNodes: List<ComparisonNodeDto>,
    val removedNodes: List<ComparisonNodeDto>,
    val modifiedNodes: List<NodeModificationDto>,
    val parameterChanges: List<ParameterChangeDto>
)

data class ComparisonNodeDto(
    val logicalId: UUID,
    val type: String,
    val title: String?,
    val description: String?,
    val path: List<String>,
    val minEffort: Double?,
    val expectedEffort: Double?,
    val maxEffort: Double?,
    val offerPT: Double?
)

data class NodeModificationDto(
    val logicalId: UUID,
    val type: String,
    val before: ComparisonNodeDto,
    val after: ComparisonNodeDto,
    val changedFields: List<String>
)

data class ParameterChangeDto(
    val name: String,
    val oldValue: Double?,
    val newValue: Double?,
    val changeType: String
)
