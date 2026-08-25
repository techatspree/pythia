@file:OptIn(ExperimentalJsExport::class)

package io.pythia.model

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
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
 * One scheduling unit — a root node of the estimation tree — placed on the
 * timeline.
 */
@JsExport
data class ScheduledTask(
    val logicalId: String,
    val title: String,
    /** The node's risk- and driver-loaded `offerPT`. */
    val effortPT: Double,
    /** The node's UNLOADED PERT mean — the scale the range is built on. */
    val meanPT: Double,
    /** The node's PERT variance, in PT-squared. */
    val effortVariance: Double,
    val durationDays: Double,
    val earliestStart: Double,
    val earliestFinish: Double,
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

/** One scheduling unit, resolved from a root node before the graph passes run. */
private class ScheduleUnit(
    val logicalId: String,
    val title: String,
    val effortPT: Double,
    val meanPT: Double,
    val variance: Double,
    val durationDays: Double
)

/**
 * What the forward pass carries along the longest path reaching a unit. The
 * mean and variance accumulate over that path, which is why the band never
 * double-counts two parallel critical branches.
 */
private class ForwardState(
    val earliestStart: Double,
    val earliestFinish: Double,
    val meanAccum: Double,
    val varianceAccum: Double
)

/**
 * Reduces [version] and a set of finish-to-start [dependencies] to a
 * [ProjectSchedule], with a team of [teamFte] full-time equivalents working on
 * one unit at a time.
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

    val units = version.roots.map { unitOf(it, teamFte) }
    val byId = units.associateBy { it.logicalId }
    // An edge naming a deleted root is ignored rather than thrown on, and a
    // duplicate edge must not count twice.
    val edges = dependencies
        .filter { it.fromLogicalId in byId && it.toLogicalId in byId }
        .toSet()

    val order = topologicalOrder(units, edges)
    if (order.size < units.size) {
        val reached = order.toSet()
        val stuck = units.map { it.logicalId }.filterNot { it in reached }.sorted()
        logger.debug { "schedule(): dependency cycle over ${stuck.size} unit(s): $stuck" }
        return failedSchedule(teamFte, ScheduleError(ScheduleErrorKind.CYCLE, stuck))
    }

    val predecessors = edges.groupBy({ it.toLogicalId }, { it.fromLogicalId })
    val successors = edges.groupBy({ it.fromLogicalId }, { it.toLogicalId })

    val forward = forwardPass(order, byId, predecessors)
    val projectDurationDays = forward.values.maxOfOrNull { it.earliestFinish } ?: 0.0
    val latestFinish = backwardPass(order, byId, successors, projectDurationDays)

    val tasks = units.map { unit ->
        val state = forward.getValue(unit.logicalId)
        val slack = latestFinish.getValue(unit.logicalId) - state.earliestFinish
        ScheduledTask(
            logicalId = unit.logicalId,
            title = unit.title,
            effortPT = unit.effortPT,
            meanPT = unit.meanPT,
            effortVariance = unit.variance,
            durationDays = unit.durationDays,
            earliestStart = state.earliestStart,
            earliestFinish = state.earliestFinish,
            onCriticalPath = slack < CRITICAL_SLACK_EPSILON
        )
    }

    // The band comes from the longest path's own mean and variance, NOT from
    // projectDurationDays: offerPT already carries a stdDevFactor loading, so
    // banding around it would apply the same sigma twice.
    val longest = longestPathState(order, forward)
    val expectedDurationDays = (longest?.meanAccum ?: 0.0) / teamFte
    // sqrt(variance / teamFte^2) == sqrt(variance) / teamFte — this is where the
    // PT-squared effort variance becomes a duration in days.
    val durationStdDevDays = sqrt(longest?.varianceAccum ?: 0.0) / teamFte
    val band = version.stdDevFactor * durationStdDevDays

    logger.debug {
        "schedule(): ${tasks.size} unit(s), teamFte=$teamFte, length=$projectDurationDays d, " +
            "expected=$expectedDurationDays d, sd=$durationStdDevDays d"
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

private fun unitOf(node: EstimationNode, teamFte: Double) = ScheduleUnit(
    logicalId = node.logicalId,
    title = labelOf(node),
    effortPT = node.offerPT,
    meanPT = node.mean,
    variance = node.variance,
    durationDays = node.offerPT / teamFte
)

private fun labelOf(node: EstimationNode): String = when (node) {
    is EstimationGroup -> node.title
    is EstimationItem  -> node.description
}

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
 * Kahn's algorithm. Returns the units in dependency order; a result SHORTER
 * than the input means the remainder sits on or behind a cycle — including a
 * self-edge, whose node never reaches in-degree 0. Ready units are taken in
 * `logicalId` order so the emitted order is deterministic.
 */
private fun topologicalOrder(units: List<ScheduleUnit>, edges: Set<ScheduleDependency>): List<String> {
    val inDegree = units.associate { it.logicalId to 0 }.toMutableMap()
    edges.forEach { inDegree[it.toLogicalId] = inDegree.getValue(it.toLogicalId) + 1 }
    val successors = edges.groupBy({ it.fromLogicalId }, { it.toLogicalId })

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

private fun forwardPass(
    order: List<String>,
    byId: Map<String, ScheduleUnit>,
    predecessors: Map<String, List<String>>
): Map<String, ForwardState> {
    val forward = LinkedHashMap<String, ForwardState>()
    order.forEach { id ->
        val unit = byId.getValue(id)
        var best: ForwardState? = null
        predecessors[id].orEmpty().sorted().forEach { predId ->
            forward[predId]?.let { best = longerPath(best, it) }
        }
        val start = best?.earliestFinish ?: 0.0
        forward[id] = ForwardState(
            earliestStart = start,
            earliestFinish = start + unit.durationDays,
            meanAccum = (best?.meanAccum ?: 0.0) + unit.meanPT,
            varianceAccum = (best?.varianceAccum ?: 0.0) + unit.variance
        )
    }
    return forward
}

private fun backwardPass(
    order: List<String>,
    byId: Map<String, ScheduleUnit>,
    successors: Map<String, List<String>>,
    projectDurationDays: Double
): Map<String, Double> {
    val latestFinish = HashMap<String, Double>()
    order.asReversed().forEach { id ->
        val next = successors[id].orEmpty()
        latestFinish[id] = if (next.isEmpty()) {
            projectDurationDays
        } else {
            next.minOf { latestFinish.getValue(it) - byId.getValue(it).durationDays }
        }
    }
    return latestFinish
}

/** The state of the unit that finishes last — the end of the longest path. */
private fun longestPathState(order: List<String>, forward: Map<String, ForwardState>): ForwardState? {
    var longest: ForwardState? = null
    order.sorted().forEach { id -> longest = longerPath(longest, forward.getValue(id)) }
    return longest
}

/**
 * Longest path wins; on a tie the larger accumulated variance wins, so the
 * reported band is the conservative one. Callers iterate in `logicalId` order
 * and only a STRICTLY better candidate replaces the incumbent, which settles
 * any remaining tie deterministically.
 */
private fun longerPath(current: ForwardState?, candidate: ForwardState): ForwardState {
    if (current == null) return candidate
    if (candidate.earliestFinish > current.earliestFinish + CRITICAL_SLACK_EPSILON) return candidate
    if (candidate.earliestFinish < current.earliestFinish - CRITICAL_SLACK_EPSILON) return current
    return if (candidate.varianceAccum > current.varianceAccum) candidate else current
}
