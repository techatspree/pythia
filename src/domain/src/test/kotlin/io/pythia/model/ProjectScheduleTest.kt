package io.pythia.model

import io.pythia.StandardMethods
import io.pythia.method.threepoint.FixedEstimationItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Lives in the AGGREGATOR `:domain`, not in `:domain:core` where
 * `ProjectSchedule.kt` itself sits: a schedule with real durations needs roots
 * carrying a real `offerPT`, which needs a concrete leaf, and `FixedEstimationItem`
 * lives in `:domain:method-threepoint` — a module core cannot see.
 */
class ProjectScheduleTest {

    private val delta = 1e-9

    @BeforeEach
    fun ensureRegistryPopulated() {
        // The aggregator's suite can see every method, so it installs the real
        // standard set (the registry no longer self-populates — task-143).
        StandardMethods.installAll()
    }

    private fun leaf(id: String, min: Double, expected: Double, max: Double) = FixedEstimationItem(
        _description = "leaf $id",
        _minEffort = min,
        _expectedEffort = expected,
        _maxEffort = max,
        _logicalId = "leaf-$id"
    )

    /** A scheduling unit with NO spread, so `offerPT` equals the raw effort. */
    private fun unit(id: String, effort: Double) = EstimationGroup(
        title = "Unit $id",
        children = listOf(leaf(id, effort, effort, effort)),
        _logicalId = id
    )

    /** A scheduling unit that carries PERT variance. */
    private fun spreadUnit(id: String, min: Double, expected: Double, max: Double) = EstimationGroup(
        title = "Unit $id",
        children = listOf(leaf(id, min, expected, max)),
        _logicalId = id
    )

    private fun versionOf(vararg roots: EstimationNode) = EstimationVersion(
        versionNumber = 1,
        stdDevFactor = 2.0,
        dailyRate = 800.0,
        salesSurcharge = 0.1,
        roots = roots.toList()
    ).calculate()

    private fun dep(from: String, to: String) = ScheduleDependency(from, to)

    private fun ProjectSchedule.task(logicalId: String) = tasks.single { it.logicalId == logicalId }

    // ---------------------------------------------------------------- durations

    @Test
    fun `a linear chain is as long as the sum of its durations`() {
        val schedule = versionOf(unit("a", 10.0), unit("b", 20.0), unit("c", 30.0))
            .schedule(listOf(dep("a", "b"), dep("b", "c")), teamFte = 1.0)

        assertNull(schedule.error)
        assertEquals(60.0, schedule.projectDurationDays, delta)
        assertEquals(0.0, schedule.task("a").earliestStart, delta)
        assertEquals(10.0, schedule.task("b").earliestStart, delta)
        assertEquals(30.0, schedule.task("c").earliestStart, delta)
        assertTrue(schedule.tasks.all { it.onCriticalPath })
    }

    @Test
    fun `the team size divides every duration`() {
        val schedule = versionOf(unit("a", 10.0), unit("b", 20.0), unit("c", 30.0))
            .schedule(listOf(dep("a", "b"), dep("b", "c")), teamFte = 2.0)

        assertEquals(30.0, schedule.projectDurationDays, delta)
        assertEquals(5.0, schedule.task("a").durationDays, delta)
    }

    @Test
    fun `two parallel branches of equal length are BOTH critical`() {
        // The case a "trace one longest chain" implementation silently gets
        // wrong: it reports only one of the two branches.
        val schedule = versionOf(unit("s", 5.0), unit("x", 10.0), unit("y", 10.0))
            .schedule(listOf(dep("s", "x"), dep("s", "y")), teamFte = 1.0)

        assertEquals(15.0, schedule.projectDurationDays, delta)
        assertTrue(schedule.task("x").onCriticalPath)
        assertTrue(schedule.task("y").onCriticalPath)
        assertTrue(schedule.task("s").onCriticalPath)
    }

    @Test
    fun `a diamond takes the longest path, not the sum of everything`() {
        val schedule = versionOf(unit("a", 10.0), unit("b", 20.0), unit("c", 5.0), unit("d", 10.0))
            .schedule(listOf(dep("a", "b"), dep("a", "c"), dep("b", "d"), dep("c", "d")), teamFte = 1.0)

        // Sum of all four would be 45; the longest path a→b→d is 40.
        assertEquals(40.0, schedule.projectDurationDays, delta)
        assertEquals(30.0, schedule.task("d").earliestStart, delta)
        assertTrue(schedule.task("a").onCriticalPath)
        assertTrue(schedule.task("b").onCriticalPath)
        assertTrue(schedule.task("d").onCriticalPath)
        assertFalse(schedule.task("c").onCriticalPath, "c has 15 days of slack")
    }

    @Test
    fun `a root that is a LEAF is labelled by its description`() {
        val schedule = versionOf(leaf("solo", 4.0, 4.0, 4.0)).schedule(emptyList(), teamFte = 1.0)

        assertEquals("leaf solo", schedule.tasks.single().title)
    }

    // ------------------------------------------------------------- bad inputs

    @Test
    fun `an edge naming an unknown unit is ignored, not thrown on`() {
        // A root can be deleted after an edge to it was drawn.
        val schedule = versionOf(unit("a", 10.0), unit("b", 20.0))
            .schedule(listOf(dep("a", "b"), dep("ghost", "a"), dep("b", "ghost")), teamFte = 1.0)

        assertNull(schedule.error)
        assertEquals(30.0, schedule.projectDurationDays, delta)
    }

    @Test
    fun `a duplicate edge changes nothing`() {
        val version = versionOf(unit("a", 10.0), unit("b", 20.0))
        val once = version.schedule(listOf(dep("a", "b")), teamFte = 1.0)
        val twice = version.schedule(listOf(dep("a", "b"), dep("a", "b")), teamFte = 1.0)

        assertEquals(once, twice)
    }

    @Test
    fun `a self edge is reported as a cycle, never looped on`() {
        val schedule = versionOf(unit("a", 10.0)).schedule(listOf(dep("a", "a")), teamFte = 1.0)

        assertEquals(ScheduleErrorKind.CYCLE, schedule.error?.kind)
        assertEquals(listOf("a"), schedule.error?.involvedLogicalIds)
        assertTrue(schedule.tasks.isEmpty())
    }

    @Test
    fun `a dependency cycle is a reported result, not a throw`() {
        val schedule = versionOf(unit("a", 10.0), unit("b", 20.0))
            .schedule(listOf(dep("a", "b"), dep("b", "a")), teamFte = 1.0)

        assertEquals(ScheduleErrorKind.CYCLE, schedule.error?.kind)
        assertEquals(listOf("a", "b"), schedule.error?.involvedLogicalIds)
        assertTrue(schedule.tasks.isEmpty())
        assertEquals(0.0, schedule.projectDurationDays, delta)
    }

    @Test
    fun `a team size of zero is reported instead of dividing`() {
        val schedule = versionOf(unit("a", 10.0)).schedule(emptyList(), teamFte = 0.0)

        assertEquals(ScheduleErrorKind.INVALID_TEAM_FTE, schedule.error?.kind)
        assertTrue(schedule.tasks.isEmpty())
        // Infinity must never reach the UI.
        assertEquals(0.0, schedule.projectDurationDays, delta)
        assertEquals(0.0, schedule.pessimisticDurationDays, delta)
    }

    // ----------------------------------------------------------------- the band

    @Test
    fun `variances ADD along a chain while standard deviations do not`() {
        // a: mean 3, variance ((6-0)/6)^2 = 1 | b: mean 6, variance ((12-0)/6)^2 = 4
        val schedule = versionOf(spreadUnit("a", 0.0, 3.0, 6.0), spreadUnit("b", 0.0, 6.0, 12.0))
            .schedule(listOf(dep("a", "b")), teamFte = 1.0)

        assertEquals(9.0, schedule.expectedDurationDays, delta)
        assertEquals(sqrt(5.0), schedule.durationStdDevDays, delta)
        // Adding the standard deviations instead of the variances gives 1+2=3.
        assertTrue(
            abs(schedule.durationStdDevDays - 3.0) > 0.5,
            "standard deviations must not be added: got ${schedule.durationStdDevDays}"
        )
    }

    @Test
    fun `the band divides effort variance by teamFte SQUARED`() {
        val version = versionOf(spreadUnit("a", 0.0, 3.0, 6.0), spreadUnit("b", 0.0, 6.0, 12.0))
        val deps = listOf(dep("a", "b"))

        val solo = version.schedule(deps, teamFte = 1.0)
        val pair = version.schedule(deps, teamFte = 2.0)

        // sqrt(v / fte^2) == sqrt(v) / fte, so doubling the team HALVES the
        // spread. Dividing the variance by fte once would give sqrt(5)/sqrt(2).
        assertEquals(solo.durationStdDevDays / 2.0, pair.durationStdDevDays, delta)
        assertEquals(sqrt(5.0) / 2.0, pair.durationStdDevDays, delta)
        assertEquals(4.5, pair.expectedDurationDays, delta)
    }

    @Test
    fun `parallel critical branches do not sum their variances`() {
        // Equal means (3.0) so both are critical; different variances (1 and
        // 4/9) so the tie-break is observable.
        val schedule = versionOf(spreadUnit("p", 0.0, 3.0, 6.0), spreadUnit("q", 1.0, 3.0, 5.0))
            .schedule(emptyList(), teamFte = 1.0)

        assertTrue(schedule.tasks.all { it.onCriticalPath })
        assertEquals(3.0, schedule.expectedDurationDays, delta)
        // One branch's spread, not sqrt(1 + 4/9) = 1.202.
        assertEquals(1.0, schedule.durationStdDevDays, delta)
    }

    @Test
    fun `the risk loaded plan length sits at the TOP of the band, never at its middle`() {
        // One unit: mean 3, variance 1, so riskFactor = sqrt(1)*2/3 and
        // offerPT = 3 + 2 = 5 — the mean plus stdDevFactor standard deviations.
        val schedule = versionOf(spreadUnit("a", 0.0, 3.0, 6.0)).schedule(emptyList(), teamFte = 1.0)

        assertEquals(3.0, schedule.expectedDurationDays, delta)
        assertEquals(1.0, schedule.durationStdDevDays, delta)
        assertEquals(1.0, schedule.optimisticDurationDays, delta)
        assertEquals(5.0, schedule.pessimisticDurationDays, delta)
        // THE point of building the band on the mean scale: the loaded figure
        // lands on the pessimistic end, not two more sigma beyond it.
        assertEquals(5.0, schedule.projectDurationDays, delta)
    }

    @Test
    fun `the optimistic end is floored at zero`() {
        // mean 16/6, variance 4 -> sd 2, band 4, so expected - band is negative.
        val schedule = versionOf(spreadUnit("a", 0.0, 1.0, 12.0)).schedule(emptyList(), teamFte = 1.0)

        assertEquals(2.0, schedule.durationStdDevDays, delta)
        assertEquals(0.0, schedule.optimisticDurationDays, delta)
    }

    @Test
    fun `a version with no roots yields an empty schedule without an error`() {
        val schedule = versionOf().schedule(emptyList(), teamFte = 1.0)

        assertNull(schedule.error)
        assertTrue(schedule.tasks.isEmpty())
        assertEquals(0.0, schedule.projectDurationDays, delta)
    }
}
