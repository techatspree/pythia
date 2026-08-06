package io.github.theestimator.model.mutation

import io.github.theestimator.method.bucketsampled.BucketedEstimationItem
import io.github.theestimator.method.threepoint.TimeRelativeEstimationItem
import io.github.theestimator.model.AdditionalCost
import io.github.theestimator.model.EffortDriver
import io.github.theestimator.model.EstimationGroup
import io.github.theestimator.model.EstimationItem
import io.github.theestimator.model.EstimationNode
import io.github.theestimator.model.EstimationVersion
import io.github.theestimator.model.ProjectPhase
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

// Wire cap: at most CAP entries. Beyond that the last is a Truncated marker.
private const val CAP = 25

/**
 * Structured, human-readable diff between two [EstimationVersion] snapshots
 * (task-110) — the single source of truth for the "Verlauf" panel. Deterministic
 * walk order: parameters → effort drivers → phases → additional costs → notes →
 * tree (roots recursively, matched by stable `logicalId`). Numeric values are
 * stringified locale-neutrally; the frontend formats them per locale.
 */
fun EstimationVersion.diffSummary(other: EstimationVersion): List<ChangeDescription> {
    val raw = mutableListOf<ChangeDescription>()

    diffParameters(other, raw)
    diffDrivers(effortDrivers, other.effortDrivers, raw)
    diffPhases(phases, other.phases, raw)
    diffCosts(additionalCosts, other.additionalCosts, raw)
    if (notes != other.notes) raw += NotesChanged(notes.ifBlank { null }, other.notes.ifBlank { null })
    diffTree(roots, other.roots, raw)

    val result = if (raw.size <= CAP) raw else raw.take(CAP - 1) + Truncated(raw.size - (CAP - 1))
    logger.debug { "diffSummary produced ${result.size} entries (raw ${raw.size}, cap $CAP)" }
    return result
}

private fun EstimationVersion.diffParameters(other: EstimationVersion, out: MutableList<ChangeDescription>) {
    if (dailyRate != other.dailyRate) out += ParameterChanged("dailyRate", fmt(dailyRate), fmt(other.dailyRate))
    if (stdDevFactor != other.stdDevFactor) {
        out += ParameterChanged("stdDevFactor", fmt(stdDevFactor), fmt(other.stdDevFactor))
    }
    if (salesSurcharge != other.salesSurcharge) {
        out += ParameterChanged("salesSurcharge", fmt(salesSurcharge), fmt(other.salesSurcharge))
    }
}

private fun diffDrivers(
    before: List<EffortDriver>,
    after: List<EffortDriver>,
    out: MutableList<ChangeDescription>
) = pair(
    before, after, keyOf = { it.id ?: it.description },
    onAdded = { out += EffortDriverAdded(it.description) },
    onRemoved = { out += EffortDriverRemoved(it.description) },
    onMatched = { b, a ->
        if (b.factor != a.factor) out += EffortDriverChanged(a.description, "factor", fmt(b.factor), fmt(a.factor))
        if (b.comment != a.comment) {
            out += EffortDriverChanged(a.description, "comment", b.comment.ifBlank { null }, a.comment.ifBlank { null })
        }
        if (b.description != a.description) {
            out += EffortDriverChanged(a.description, "description", b.description, a.description)
        }
    }
)

private fun diffPhases(
    before: List<ProjectPhase>,
    after: List<ProjectPhase>,
    out: MutableList<ChangeDescription>
) = pair(
    before, after, keyOf = { it.abbreviation },
    onAdded = { out += PhaseAdded(it.name) },
    onRemoved = { out += PhaseRemoved(it.name) },
    onMatched = { b, a ->
        if (b.name != a.name) out += PhaseChanged(a.name, "name", b.name, a.name)
        if (b.durationWeeks != a.durationWeeks) {
            out += PhaseChanged(a.name, "durationWeeks", fmt(b.durationWeeks), fmt(a.durationWeeks))
        }
    }
)

private fun diffCosts(
    before: List<AdditionalCost>,
    after: List<AdditionalCost>,
    out: MutableList<ChangeDescription>
) = pair(
    before, after, keyOf = { it.id ?: it.description },
    onAdded = { out += AdditionalCostAdded(it.description) },
    onRemoved = { out += AdditionalCostRemoved(it.description) },
    onMatched = { b, a ->
        if (b.amount != a.amount) out += AdditionalCostChanged(a.description, "amount", fmt(b.amount), fmt(a.amount))
        if (b.amountPerWeek != a.amountPerWeek) {
            out += AdditionalCostChanged(a.description, "amountPerWeek", fmt(b.amountPerWeek), fmt(a.amountPerWeek))
        }
        if (b.type != a.type) out += AdditionalCostChanged(a.description, "type", b.type.name, a.type.name)
        if (b.description != a.description) {
            out += AdditionalCostChanged(a.description, "description", b.description, a.description)
        }
    }
)

// A node in one tree, with its slash-joined path and parent path (for move
// detection) and its wire node-type discriminator.
private class NodeInfo(val node: EstimationNode, val path: String, val parentPath: String, val nodeType: String)

private fun diffTree(
    before: List<EstimationNode>,
    after: List<EstimationNode>,
    out: MutableList<ChangeDescription>
) {
    val beforeById = indexTree(before)
    val afterById = indexTree(after)
    // Added + matched, in after-walk order (deterministic).
    for ((id, ai) in afterById) {
        val bi = beforeById[id]
        if (bi == null) {
            out += NodeAdded(ai.path, ai.nodeType)
            continue
        }
        val oldTitle = titleOf(bi.node)
        val newTitle = titleOf(ai.node)
        if (oldTitle != newTitle) out += NodeRenamed(bi.path, oldTitle, newTitle)
        if (bi.parentPath != ai.parentPath) out += NodeMoved(bi.path, ai.path)
        diffLeafValues(bi.node, ai.node, ai.path, out)
    }
    // Removed, in before-walk order.
    for ((id, bi) in beforeById) {
        if (!afterById.containsKey(id)) out += NodeRemoved(bi.path, bi.nodeType)
    }
}

private fun indexTree(roots: List<EstimationNode>): Map<String, NodeInfo> {
    val map = LinkedHashMap<String, NodeInfo>()
    fun walk(node: EstimationNode, parentPath: String) {
        val path = if (parentPath.isEmpty()) titleOf(node) else "$parentPath/${titleOf(node)}"
        map[node.logicalId] = NodeInfo(node, path, parentPath, nodeTypeOf(node))
        if (node is EstimationGroup) node.children.forEach { walk(it, path) }
    }
    roots.forEach { walk(it, "") }
    return map
}

private fun diffLeafValues(
    before: EstimationNode,
    after: EstimationNode,
    path: String,
    out: MutableList<ChangeDescription>
) {
    if (before is BucketedEstimationItem && after is BucketedEstimationItem) {
        cmpStr(path, "bucket", before.bucketId, after.bucketId, out)
        cmpStr(path, "isSample", before.isSample.toString(), after.isSample.toString(), out)
        cmpNum(path, "optimistic", before.optimistic, after.optimistic, out)
        cmpNum(path, "likely", before.likely, after.likely, out)
        cmpNum(path, "pessimistic", before.pessimistic, after.pessimistic, out)
    } else if (before is EstimationItem && after is EstimationItem) {
        cmpNum(path, "optimistic", before.minEffort, after.minEffort, out)
        cmpNum(path, "likely", before.expectedEffort, after.expectedEffort, out)
        cmpNum(path, "pessimistic", before.maxEffort, after.maxEffort, out)
        cmpStr(path, "assumptions", before.assumptions.ifBlank { null }, after.assumptions.ifBlank { null }, out)
        cmpStr(path, "phase", before.phase?.abbreviation, after.phase?.abbreviation, out)
        if (before is TimeRelativeEstimationItem && after is TimeRelativeEstimationItem) {
            cmpStr(path, "unit", before.unit, after.unit, out)
        }
    }
}

private fun cmpStr(path: String, field: String, old: String?, new: String?, out: MutableList<ChangeDescription>) {
    if (old != new) out += NodeValueChanged(path, field, old, new)
}

private fun cmpNum(path: String, field: String, old: Double?, new: Double?, out: MutableList<ChangeDescription>) {
    if (old != new) out += NodeValueChanged(path, field, fmt(old), fmt(new))
}

private fun titleOf(node: EstimationNode): String = when (node) {
    is EstimationGroup -> node.title
    is EstimationItem -> node.description
}

private fun nodeTypeOf(node: EstimationNode): String = when (node) {
    is EstimationGroup -> "GROUP"
    is BucketedEstimationItem -> "BUCKETED"
    is TimeRelativeEstimationItem -> "TIME_RELATIVE"
    is EstimationItem -> "FIXED"
}

// Locale-NEUTRAL numeric string: whole numbers without a fractional part, the
// rest via the platform toString (always a `.` decimal). The frontend re-formats
// numeric values per locale via $lib/format.ts; never format for a locale here.
private fun fmt(v: Double?): String? {
    if (v == null) return null
    return if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
}

// id-preferring pairing of two collections: matched keys yield onMatched, keys
// only in `after` yield onAdded (in after order), keys only in `before` yield
// onRemoved (in before order). One helper, not inlined per collection.
private inline fun <T> pair(
    before: List<T>,
    after: List<T>,
    keyOf: (T) -> String,
    onAdded: (T) -> Unit,
    onRemoved: (T) -> Unit,
    onMatched: (before: T, after: T) -> Unit
) {
    val beforeByKey = before.associateBy(keyOf)
    val afterByKey = after.associateBy(keyOf)
    for (a in after) {
        val b = beforeByKey[keyOf(a)]
        if (b == null) onAdded(a) else onMatched(b, a)
    }
    for (b in before) {
        if (!afterByKey.containsKey(keyOf(b))) onRemoved(b)
    }
}
