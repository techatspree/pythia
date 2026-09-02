@file:OptIn(ExperimentalJsExport::class)
// One cohesive algorithm — tree indexing, edge lowering, the two graph passes
// and the roll-up — split into small private helpers rather than one long
// function. Keeping them in the file that defines the types they operate on is
// clearer than a second file, so the file-level function count is intentional.
@file:Suppress("TooManyFunctions")

package io.pythia.model

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

private val logger = KotlinLogging.logger {}

/**
 * Slack below which a unit counts as critical. Durations are floating point and
 * accumulate along a path, so an exact `latestFinish == earliestFinish` test
 * would drop units that are critical but carry rounding.
 */
private const val CRITICAL_SLACK_EPSILON = 1e-9

/** A finish-to-start edge between two scheduling units, by `logicalId`. */
@JsExport
data class ScheduleDependency(val fromLogicalId: String, val toLogicalId: String)

/**
 * One node of the estimation tree placed on the timeline — groups included
 * (task-164). The list covers EVERY node and is emitted in tree order, so a
 * consumer rebuilds the hierarchy from [parentLogicalId]/[depth] without
 * walking `EstimationVersion` again and without sorting.
 */
@JsExport
data class ScheduledTask(
    val logicalId: String,
    val title: String,
    /** Null for a top-level node. */
    val parentLogicalId: String?,
    /** 0 for a root; +1 per level. */
    val depth: Int,
    /** True when this node has children and its dates are a ROLL-UP of them. */
    val isGroup: Boolean,
    /** The node's risk- and driver-loaded `offerPT`; for a group, its subtree sum. */
    val effortPT: Double,
    /** The node's UNLOADED PERT mean — the scale the range is built on. */
    val meanPT: Double,
    /** The node's PERT variance, in PT-squared. */
    val effortVariance: Double,
    /**
     * For a LEAF, its own duration. For a GROUP, its rolled-up SPAN
     * (`earliestFinish - earliestStart`) — deliberately NOT an effort quotient,
     * which differs whenever its children overlap.
     */
    val durationDays: Double,
    val earliestStart: Double,
    val earliestFinish: Double,
    /** For a group, true when ANY leaf in its subtree is critical. */
    val onCriticalPath: Boolean
)

/** Why a schedule could not be produced. */
@JsExport
enum class ScheduleErrorKind { CYCLE, INVALID_TEAM_FTE }

/**
 * Structured on purpose — task-157 maps [kind] to an i18n key and interpolates
 * the unit titles itself. A composed English sentence could not be translated.
 */
@JsExport
data class ScheduleError(
    val kind: ScheduleErrorKind,
    val involvedLogicalIds: List<String>
)

/**
 * A rough project schedule derived from one estimation version.
 *
 * [projectDurationDays] is the risk-loaded plan length, and it sits near the
 * TOP of the [optimisticDurationDays]…[pessimisticDurationDays] band by
 * construction — `offerPT` already contains a `stdDevFactor` loading, while the
 * band is computed on the unloaded mean scale. It is not the band's midpoint
 * and must not be presented as one.
 */
@JsExport
data class ProjectSchedule(
    val tasks: List<ScheduledTask>,
    /** Risk-loaded plan length. Sits near the TOP of the band below. */
    val projectDurationDays: Double,
    val expectedDurationDays: Double,
    val durationStdDevDays: Double,
    val optimisticDurationDays: Double,
    val pessimisticDurationDays: Double,
    val teamFte: Double,
    /** Non-null when the inputs are unusable; `tasks` is then empty. */
    val error: ScheduleError?
)

/**
 * One node of the estimation tree, indexed with its place in the hierarchy.
 * Groups are NOT scheduled — they roll up from their leaves (task-164).
 */
private class TreeNode(
    val node: EstimationNode,
    val parentLogicalId: String?,
    val depth: Int,
    /** Every leaf beneath this node; the node itself when it is a leaf. */
    val subtreeLeafIds: List<String>
) {
    // Derived rather than copied: EstimationNode already accumulates these over
    // its subtree, so duplicating them into fields would be a second copy to
    // keep in step (and nine constructor parameters).
    val logicalId: String get() = node.logicalId
    val title: String get() = labelOf(node)
    val isGroup: Boolean get() = node is EstimationGroup
    val effortPT: Double get() = node.offerPT
    val meanPT: Double get() = node.mean
    val variance: Double get() = node.variance
}

/**
 * A node of the SCHEDULING graph: either a real leaf, or one of the two
 * zero-duration milestones that stand in for a group.
 */
private class GraphNode(
    val id: String,
    val durationDays: Double,
    val meanPT: Double,
    val variance: Double,
    /**
     * A group's `#start`/`#finish` stand-in: it respects its predecessors but
     * consumes NO worker. Flagged explicitly rather than inferred from a zero
     * duration — a real leaf may legitimately have zero effort.
     */
    val isMilestone: Boolean
)

/**
 * What the forward pass carries along the longest path reaching a node. The
 * mean and variance accumulate over that path, which is why the band never
 * double-counts two parallel critical branches.
 */
private class ForwardState(
    /** Levelled: when a worker was actually free. */
    val earliestStart: Double,
    val earliestFinish: Double,
    /**
     * The finish IGNORING capacity — the end of the longest dependency path to
     * this node. The uncertainty band is read off this, not off the levelled
     * dates: after levelling a node can finish last because the team was busy,
     * which says nothing about estimate uncertainty.
     */
    val dependencyFinish: Double,
    val meanAccum: Double,
    val varianceAccum: Double
)

// Milestone ids. A group `g` becomes `g#start` / `g#finish` in the scheduling
// graph. logicalIds are generated UUIDs, so `#` cannot collide with a real one.
private fun startNodeOf(id: String, isGroup: Boolean) = if (isGroup) "$id#start" else id
private fun finishNodeOf(id: String, isGroup: Boolean) = if (isGroup) "$id#finish" else id

/** Maps a scheduling-graph id back to the tree node it belongs to. */
private fun ownerOf(graphId: String): String = graphId.substringBefore('#')

/**
 * Reduces [version] and a set of finish-to-start [dependencies] to a
 * [ProjectSchedule], with a team of [teamFte] full-time equivalents.
 *
 * **Call this on the result of [EstimationVersion.calculate]** — a node's
 * `offerPT` derives from the `CalculationParameters` that `calculate()` stamps
 * on, so on an uncalculated version every duration is 0.
 */
internal fun computeSchedule(
    version: EstimationVersion,
    dependencies: List<ScheduleDependency>,
    teamFte: Double
): ProjectSchedule {
    if (teamFte <= 0.0) {
        logger.debug { "schedule(): teamFte=$teamFte is not a usable team size, no schedule computed" }
        return failedSchedule(teamFte, ScheduleError(ScheduleErrorKind.INVALID_TEAM_FTE, emptyList()))
    }

    // Tree order (depth-first, root order) is the emission order consumers rely on.
    val tree = indexTree(version.roots)
    if (tree.isEmpty()) return emptySchedule(teamFte)
    val byId = tree.associateBy { it.logicalId }

    // An edge naming a node that no longer exists is ignored rather than thrown
    // on; a duplicate edge must not count twice.
    val drawn = dependencies
        .filter { it.fromLogicalId in byId && it.toLogicalId in byId }
        .toSet()

    val graph = buildGraph(tree, byId, drawn)
    val order = topologicalOrder(graph.nodes.map { it.id }, graph.edges)
    if (order.size < graph.nodes.size) {
        val stuck = cycleIdsInUserTerms(graph.nodes.map { it.id } - order.toSet(), drawn)
        logger.debug { "schedule(): dependency cycle over ${stuck.size} node(s): $stuck" }
        return failedSchedule(teamFte, ScheduleError(ScheduleErrorKind.CYCLE, stuck))
    }

    val nodeById = graph.nodes.associateBy { it.id }
    val predecessors = graph.edges.groupBy({ it.second }, { it.first })
    val successors = graph.edges.groupBy({ it.first }, { it.second })

    // teamFte is a WORKER COUNT (task-166). A fractional value rounds down, with
    // a floor of one, so 2.5 people schedule as 2 concurrent workers.
    val slots = max(1, floor(teamFte).toInt())
    val forward = forwardPass(order, nodeById, predecessors, slots)
    val projectDurationDays = forward.values.maxOfOrNull { it.earliestFinish } ?: 0.0
    val latestFinish = backwardPass(order, nodeById, successors, projectDurationDays)

    val criticalLeaves = tree.filterNot { it.isGroup }
        .filter { leaf ->
            val slack = latestFinish.getValue(leaf.logicalId) -
                forward.getValue(leaf.logicalId).earliestFinish
            slack < CRITICAL_SLACK_EPSILON
        }
        .map { it.logicalId }
        .toSet()

    val tasks = tree.map { node -> scheduledTaskFor(node, forward, criticalLeaves) }

    // The band follows the longest DEPENDENCY path's own mean and variance, not
    // projectDurationDays: offerPT already carries a stdDevFactor loading, so
    // banding around it would apply the same sigma twice. Milestones contribute
    // nothing, so reading the leaf states is enough.
    val longest = longestLeafState(tree, forward)
    // No `/ teamFte`: a leaf now takes effortPT DAYS, so the band is already in
    // days. Dividing would report it teamFte times too small.
    val expectedDurationDays = longest?.meanAccum ?: 0.0
    val durationStdDevDays = sqrt(longest?.varianceAccum ?: 0.0)
    val band = version.stdDevFactor * durationStdDevDays

    val leafCount = tree.count { !it.isGroup }
    val totalEffort = tree.filterNot { it.isGroup }.sumOf { it.effortPT }
    logger.debug {
        "schedule(): ${tree.size} node(s) ($leafCount leaves), " +
            "${drawn.size} drawn edge(s) lowered to ${graph.edges.size}, " +
            "teamFte=$teamFte -> $slots worker slot(s), totalEffort=$totalEffort PT, " +
            "makespan=$projectDurationDays d, expected=$expectedDurationDays d, sd=$durationStdDevDays d"
    }

    return ProjectSchedule(
        tasks = tasks,
        projectDurationDays = projectDurationDays,
        expectedDurationDays = expectedDurationDays,
        durationStdDevDays = durationStdDevDays,
        optimisticDurationDays = max(0.0, expectedDurationDays - band),
        pessimisticDurationDays = expectedDurationDays + band,
        teamFte = teamFte,
        error = null
    )
}

/** Depth-first in root order — the emission order `tasks` is contracted to have. */
private fun indexTree(roots: List<EstimationNode>): List<TreeNode> {
    val out = mutableListOf<TreeNode>()
    fun visit(node: EstimationNode, parentId: String?, depth: Int) {
        val children = (node as? EstimationGroup)?.children.orEmpty()
        out.add(
            TreeNode(
                node = node,
                parentLogicalId = parentId,
                depth = depth,
                subtreeLeafIds = node.leaves().map { it.logicalId }.toList()
            )
        )
        children.forEach { visit(it, node.logicalId, depth + 1) }
    }
    roots.forEach { visit(it, null, 0) }
    return out
}

private class Graph(val nodes: List<GraphNode>, val edges: Set<Pair<String, String>>)

/**
 * Lowers the tree and the drawn edges onto a schedulable graph.
 *
 * Groups are NOT scheduled. Each becomes two zero-duration milestones,
 * `g#start` and `g#finish`, wired around its children. A drawn edge `A -> B`
 * lowers to `finishOf(A) -> startOf(B)`. That is O(nodes) edges; expanding to
 * "every leaf of A -> every leaf of B" would be O(leaves squared) — 2500 edges
 * for two 50-leaf groups — and the alternative of "start after A's latest
 * finish" cannot work, because that finish is unknown until A is scheduled
 * while the constraint must already be in the graph for the topological sort.
 */
private fun buildGraph(
    tree: List<TreeNode>,
    byId: Map<String, TreeNode>,
    drawn: Set<ScheduleDependency>
): Graph {
    val nodes = mutableListOf<GraphNode>()
    val edges = mutableSetOf<Pair<String, String>>()

    tree.forEach { node ->
        if (node.isGroup) {
            nodes.add(GraphNode("${node.logicalId}#start", 0.0, 0.0, 0.0, isMilestone = true))
            nodes.add(GraphNode("${node.logicalId}#finish", 0.0, 0.0, 0.0, isMilestone = true))
        } else {
            // task-166: a leaf takes effortPT DAYS, worked by ONE person. teamFte
            // is a worker COUNT, not a divisor — dividing here is what let the
            // team be cloned once per parallel branch.
            nodes.add(GraphNode(node.logicalId, node.effortPT, node.meanPT, node.variance, isMilestone = false))
        }
    }

    // Bracket every child between its parent's milestones.
    tree.filter { it.parentLogicalId != null }.forEach { child ->
        val parent = byId.getValue(child.parentLogicalId!!)
        edges.add("${parent.logicalId}#start" to startNodeOf(child.logicalId, child.isGroup))
        edges.add(finishNodeOf(child.logicalId, child.isGroup) to "${parent.logicalId}#finish")
    }

    drawn.forEach { edge ->
        val from = byId.getValue(edge.fromLogicalId)
        val to = byId.getValue(edge.toLogicalId)
        edges.add(finishNodeOf(from.logicalId, from.isGroup) to startNodeOf(to.logicalId, to.isGroup))
    }

    return Graph(nodes, edges)
}

/**
 * A cycle is reported in the terms the USER drew it in: a group edge names the
 * group, never the lowered milestones or the leaves beneath it, which the user
 * never touched.
 */
private fun cycleIdsInUserTerms(stuckGraphIds: List<String>, drawn: Set<ScheduleDependency>): List<String> {
    val owners = stuckGraphIds.map { ownerOf(it) }.toSet()
    val drawnEndpoints = drawn.flatMap { listOf(it.fromLogicalId, it.toLogicalId) }.toSet()
    val named = owners.intersect(drawnEndpoints).sorted()
    return named.ifEmpty { owners.sorted() }
}

private fun scheduledTaskFor(
    node: TreeNode,
    forward: Map<String, ForwardState>,
    criticalLeaves: Set<String>
): ScheduledTask {
    // A group's dates ROLL UP from its subtree leaves; it is not scheduled itself.
    val leafStates = node.subtreeLeafIds.mapNotNull { forward[it] }
    val start = if (node.isGroup) leafStates.minOfOrNull { it.earliestStart } ?: 0.0
    else forward.getValue(node.logicalId).earliestStart
    val finish = if (node.isGroup) leafStates.maxOfOrNull { it.earliestFinish } ?: 0.0
    else forward.getValue(node.logicalId).earliestFinish

    return ScheduledTask(
        logicalId = node.logicalId,
        title = node.title,
        parentLogicalId = node.parentLogicalId,
        depth = node.depth,
        isGroup = node.isGroup,
        effortPT = node.effortPT,
        meanPT = node.meanPT,
        effortVariance = node.variance,
        // For a group this is the SPAN, which differs from effort/teamFte
        // whenever its children overlap.
        durationDays = finish - start,
        earliestStart = start,
        earliestFinish = finish,
        onCriticalPath = node.subtreeLeafIds.any { it in criticalLeaves }
    )
}

private fun labelOf(node: EstimationNode): String = when (node) {
    is EstimationGroup -> node.title
    is EstimationItem  -> node.description
}

private fun emptySchedule(teamFte: Double) = ProjectSchedule(
    tasks = emptyList(),
    projectDurationDays = 0.0,
    expectedDurationDays = 0.0,
    durationStdDevDays = 0.0,
    optimisticDurationDays = 0.0,
    pessimisticDurationDays = 0.0,
    teamFte = teamFte,
    error = null
)

private fun failedSchedule(teamFte: Double, error: ScheduleError) = ProjectSchedule(
    tasks = emptyList(),
    projectDurationDays = 0.0,
    expectedDurationDays = 0.0,
    durationStdDevDays = 0.0,
    optimisticDurationDays = 0.0,
    pessimisticDurationDays = 0.0,
    teamFte = teamFte,
    error = error
)

/**
 * Kahn's algorithm over the scheduling graph. A result SHORTER than the input
 * means the remainder sits on or behind a cycle — including a self-edge, whose
 * node never reaches in-degree 0. Ready nodes are taken in id order so the
 * emitted order is deterministic.
 */
private fun topologicalOrder(nodeIds: List<String>, edges: Set<Pair<String, String>>): List<String> {
    val inDegree = nodeIds.associateWith { 0 }.toMutableMap()
    edges.forEach { (_, to) -> inDegree[to] = inDegree.getValue(to) + 1 }
    val successors = edges.groupBy({ it.first }, { it.second })

    val ready = ArrayDeque(inDegree.filterValues { it == 0 }.keys.sorted())
    val order = mutableListOf<String>()
    while (ready.isNotEmpty()) {
        val id = ready.removeFirst()
        order.add(id)
        successors[id].orEmpty().sorted().forEach { next ->
            val remaining = inDegree.getValue(next) - 1
            inDegree[next] = remaining
            if (remaining == 0) ready.addLast(next)
        }
    }
    return order
}

/**
 * Serial schedule generation (task-166): each node starts at the later of its
 * dependencies being met and a worker being free, and then occupies that worker.
 * Standard list scheduling under precedence + capacity — the optimal makespan is
 * NP-hard, this greedy approximation is what lightweight planners use.
 *
 * Two properties hold by construction:
 *  - **capacity is never exceeded**: at most [slots] non-milestone nodes are in
 *    flight at any instant, because a node only starts when a slot frees.
 *  - **it is deterministic**: [order] is already sorted, and ties on `freeAt`
 *    resolve to the lowest slot index.
 */
private fun forwardPass(
    order: List<String>,
    nodeById: Map<String, GraphNode>,
    predecessors: Map<String, List<String>>,
    slots: Int
): Map<String, ForwardState> {
    val forward = LinkedHashMap<String, ForwardState>()
    val freeAt = DoubleArray(slots)
    order.forEach { id ->
        val node = nodeById.getValue(id)
        val preds = predecessors[id].orEmpty().sorted().mapNotNull { forward[it] }
        val depsReady = preds.maxOfOrNull { it.earliestFinish } ?: 0.0

        // The accumulation follows the longest DEPENDENCY path, not the queue.
        var best: ForwardState? = null
        preds.forEach { best = longerPath(best, it) }

        val start = if (node.isMilestone) {
            // Zero work: respects its predecessors, occupies nobody.
            depsReady
        } else {
            val slot = freeAt.indices.minByOrNull { freeAt[it] } ?: 0
            val begin = max(depsReady, freeAt[slot])
            freeAt[slot] = begin + node.durationDays
            begin
        }

        forward[id] = ForwardState(
            earliestStart = start,
            earliestFinish = start + node.durationDays,
            dependencyFinish = (best?.dependencyFinish ?: 0.0) + node.durationDays,
            meanAccum = (best?.meanAccum ?: 0.0) + node.meanPT,
            varianceAccum = (best?.varianceAccum ?: 0.0) + node.variance
        )
    }
    return forward
}

private fun backwardPass(
    order: List<String>,
    nodeById: Map<String, GraphNode>,
    successors: Map<String, List<String>>,
    projectDurationDays: Double
): Map<String, Double> {
    val latestFinish = HashMap<String, Double>()
    order.asReversed().forEach { id ->
        val next = successors[id].orEmpty()
        latestFinish[id] = if (next.isEmpty()) {
            projectDurationDays
        } else {
            next.minOf { latestFinish.getValue(it) - nodeById.getValue(it).durationDays }
        }
    }
    return latestFinish
}

/**
 * The leaf at the end of the longest DEPENDENCY path. Selected on
 * `dependencyFinish`, not the levelled finish: after levelling a leaf can be
 * last because a worker was busy, which is a resource fact, not an estimate one.
 */
private fun longestLeafState(tree: List<TreeNode>, forward: Map<String, ForwardState>): ForwardState? {
    var longest: ForwardState? = null
    tree.filterNot { it.isGroup }
        .map { it.logicalId }
        .sorted()
        .forEach { id -> forward[id]?.let { longest = longerPath(longest, it) } }
    return longest
}

/**
 * Longest path wins; on a tie the larger accumulated variance wins, so the
 * reported band is the conservative one. Callers iterate in id order and only a
 * STRICTLY better candidate replaces the incumbent, which settles any remaining
 * tie deterministically.
 */
private fun longerPath(current: ForwardState?, candidate: ForwardState): ForwardState {
    if (current == null) return candidate
    if (candidate.dependencyFinish > current.dependencyFinish + CRITICAL_SLACK_EPSILON) return candidate
    if (candidate.dependencyFinish < current.dependencyFinish - CRITICAL_SLACK_EPSILON) return current
    return if (candidate.varianceAccum > current.varianceAccum) candidate else current
}
