package io.github.theestimator.service

import io.github.theestimator.domain.submitted.SubmittedEstimationNode
import io.github.theestimator.domain.submitted.SubmittedEstimationVersion
import io.github.theestimator.domain.submitted.SubmittedFixedItemNode
import io.github.theestimator.domain.submitted.SubmittedGroupNode
import io.github.theestimator.domain.submitted.SubmittedTimeRelativeItemNode
import io.github.theestimator.rest.dto.ComparisonNodeDto
import io.github.theestimator.rest.dto.NodeModificationDto
import io.github.theestimator.rest.dto.ParameterChangeDto
import io.github.theestimator.rest.dto.VersionComparisonDto
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class VersionComparisonService {

    fun compare(versionA: SubmittedEstimationVersion, versionB: SubmittedEstimationVersion): VersionComparisonDto {
        val nodesA = flattenWithPath(versionA)
        val nodesB = flattenWithPath(versionB)

        val mapA = nodesA.associateBy { it.first.logicalId }
        val mapB = nodesB.associateBy { it.first.logicalId }

        val addedNodes = mapB.keys.subtract(mapA.keys).map { lid ->
            val (node, path) = mapB[lid]!!
            node.toComparisonDto(path)
        }

        val removedNodes = mapA.keys.subtract(mapB.keys).map { lid ->
            val (node, path) = mapA[lid]!!
            node.toComparisonDto(path)
        }

        val modifiedNodes = mapA.keys.intersect(mapB.keys).mapNotNull { lid ->
            val (nodeA, pathA) = mapA[lid]!!
            val (nodeB, pathB) = mapB[lid]!!
            val changedFields = detectChangedFields(nodeA, pathA, nodeB, pathB)
            if (changedFields.isEmpty()) null
            else NodeModificationDto(
                logicalId = lid,
                type = nodeTypeOf(nodeB),
                before = nodeA.toComparisonDto(pathA),
                after = nodeB.toComparisonDto(pathB),
                changedFields = changedFields
            )
        }

        val parameterChanges = compareParameters(versionA, versionB)

        return VersionComparisonDto(
            versionA = versionA.versionNumber,
            versionB = versionB.versionNumber,
            addedNodes = addedNodes,
            removedNodes = removedNodes,
            modifiedNodes = modifiedNodes,
            parameterChanges = parameterChanges
        )
    }

    private fun flattenWithPath(
        version: SubmittedEstimationVersion
    ): List<Pair<SubmittedEstimationNode, List<String>>> {
        val out = mutableListOf<Pair<SubmittedEstimationNode, List<String>>>()
        fun walk(node: SubmittedEstimationNode, path: List<String>) {
            out.add(node to path)
            if (node is SubmittedGroupNode) {
                val childPath = path + (node.title ?: "")
                node.children.forEach { walk(it, childPath) }
            }
        }
        version.roots.forEach { walk(it, emptyList()) }
        return out
    }

    private fun detectChangedFields(
        a: SubmittedEstimationNode, pathA: List<String>,
        b: SubmittedEstimationNode, pathB: List<String>
    ): List<String> {
        val fields = mutableListOf<String>()
        if (pathA != pathB) fields.add("parent")
        if (a is SubmittedGroupNode && b is SubmittedGroupNode) {
            if (a.title != b.title) fields.add("title")
        } else if (a !is SubmittedGroupNode && b !is SubmittedGroupNode) {
            if (a.description != b.description) fields.add("description")
            if (a.code != b.code) fields.add("code")
            if (a.minEffort != b.minEffort) fields.add("minEffort")
            if (a.expectedEffort != b.expectedEffort) fields.add("expectedEffort")
            if (a.maxEffort != b.maxEffort) fields.add("maxEffort")
            if (a.assumptions != b.assumptions) fields.add("assumptions")
        } else {
            // node_type changed between versions (group ↔ leaf). Treat as a type change.
            fields.add("type")
        }
        return fields
    }

    private fun compareParameters(
        versionA: SubmittedEstimationVersion,
        versionB: SubmittedEstimationVersion
    ): List<ParameterChangeDto> {
        // The parameters are a fixed triple now (task-138), so a comparison can
        // only ever report MODIFIED — there is nothing to add or remove.
        val pairs = listOf(
            Triple("dailyRate", versionA.dailyRate, versionB.dailyRate),
            Triple("stdDevFactor", versionA.stdDevFactor, versionB.stdDevFactor),
            Triple("salesSurcharge", versionA.salesSurcharge, versionB.salesSurcharge)
        )
        return pairs.mapNotNull { (name, oldVal, newVal) ->
            if (oldVal != newVal) ParameterChangeDto(name, oldVal, newVal, "MODIFIED") else null
        }
    }

    private fun nodeTypeOf(node: SubmittedEstimationNode): String = when (node) {
        is SubmittedGroupNode -> "GROUP"
        is SubmittedTimeRelativeItemNode -> "TIME_RELATIVE"
        is SubmittedFixedItemNode -> "FIXED"
        else -> error("Unknown submitted node type: ${node::class.simpleName}")
    }

    private fun SubmittedEstimationNode.toComparisonDto(path: List<String>): ComparisonNodeDto {
        val isGroup = this is SubmittedGroupNode
        return ComparisonNodeDto(
            logicalId = logicalId,
            type = nodeTypeOf(this),
            title = if (isGroup) title else null,
            description = if (isGroup) null else description,
            path = path,
            minEffort = if (isGroup) null else minEffort,
            expectedEffort = if (isGroup) null else expectedEffort,
            maxEffort = if (isGroup) null else maxEffort,
            offerPT = offerPT
        )
    }
}
