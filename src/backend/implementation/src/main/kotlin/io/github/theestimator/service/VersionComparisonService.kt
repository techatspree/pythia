package io.github.theestimator.service

import io.github.theestimator.domain.submitted.SubmittedEstimationItem
import io.github.theestimator.domain.submitted.SubmittedEstimationVersion
import io.github.theestimator.rest.dto.*
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class VersionComparisonService {

    fun compare(versionA: SubmittedEstimationVersion, versionB: SubmittedEstimationVersion): VersionComparisonDto {
        val itemsA = versionA.itemGroups.flatMap { group -> group.items.map { it to group.title } }
        val itemsB = versionB.itemGroups.flatMap { group -> group.items.map { it to group.title } }

        val mapA = itemsA.associateBy { it.first.logicalId }
        val mapB = itemsB.associateBy { it.first.logicalId }

        val addedItems = mapB.keys.subtract(mapA.keys).map { lid ->
            val (item, groupTitle) = mapB[lid]!!
            item.toComparisonDto(groupTitle)
        }

        val removedItems = mapA.keys.subtract(mapB.keys).map { lid ->
            val (item, groupTitle) = mapA[lid]!!
            item.toComparisonDto(groupTitle)
        }

        val modifiedItems = mapA.keys.intersect(mapB.keys).mapNotNull { lid ->
            val (itemA, groupTitleA) = mapA[lid]!!
            val (itemB, groupTitleB) = mapB[lid]!!
            val changedFields = detectChangedFields(itemA, itemB)
            if (changedFields.isEmpty()) null
            else ItemModificationDto(
                logicalId = lid,
                description = itemB.description,
                before = itemA.toComparisonDto(groupTitleA),
                after = itemB.toComparisonDto(groupTitleB),
                changedFields = changedFields
            )
        }

        val groupsA = versionA.itemGroups.associateBy { it.logicalId }
        val groupsB = versionB.itemGroups.associateBy { it.logicalId }

        val addedGroups = groupsB.keys.subtract(groupsA.keys).map { lid ->
            ComparisonGroupDto(lid, groupsB[lid]!!.title)
        }

        val removedGroups = groupsA.keys.subtract(groupsB.keys).map { lid ->
            ComparisonGroupDto(lid, groupsA[lid]!!.title)
        }

        val parameterChanges = compareParameters(versionA, versionB)

        return VersionComparisonDto(
            versionA = versionA.versionNumber,
            versionB = versionB.versionNumber,
            addedItems = addedItems,
            removedItems = removedItems,
            modifiedItems = modifiedItems,
            addedGroups = addedGroups,
            removedGroups = removedGroups,
            parameterChanges = parameterChanges
        )
    }

    private fun detectChangedFields(a: SubmittedEstimationItem, b: SubmittedEstimationItem): List<String> {
        val fields = mutableListOf<String>()
        if (a.description != b.description) fields.add("description")
        if (a.code != b.code) fields.add("code")
        if (a.minEffort != b.minEffort) fields.add("minEffort")
        if (a.expectedEffort != b.expectedEffort) fields.add("expectedEffort")
        if (a.maxEffort != b.maxEffort) fields.add("maxEffort")
        if (a.assumptions != b.assumptions) fields.add("assumptions")
        return fields
    }

    private fun compareParameters(versionA: SubmittedEstimationVersion, versionB: SubmittedEstimationVersion): List<ParameterChangeDto> {
        val paramsA = versionA.parameters.associate { it.name to it.value }
        val paramsB = versionB.parameters.associate { it.name to it.value }

        val allNames = paramsA.keys + paramsB.keys
        return allNames.mapNotNull { name ->
            val oldVal = paramsA[name]
            val newVal = paramsB[name]
            when {
                oldVal == null && newVal != null -> ParameterChangeDto(name, null, newVal, "ADDED")
                oldVal != null && newVal == null -> ParameterChangeDto(name, oldVal, null, "REMOVED")
                oldVal != newVal -> ParameterChangeDto(name, oldVal, newVal, "MODIFIED")
                else -> null
            }
        }
    }

    private fun SubmittedEstimationItem.toComparisonDto(groupTitle: String) = ComparisonItemDto(
        logicalId = logicalId,
        description = description,
        minEffort = minEffort,
        expectedEffort = expectedEffort,
        maxEffort = maxEffort,
        offerPT = offerPT,
        groupTitle = groupTitle
    )
}
