package io.pythia.model

import io.pythia.StandardMethods
import io.pythia.method.threepoint.FixedEstimationItem
import io.pythia.method.threepoint.TimeRelativeEstimationItem
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

    private fun leafId(id: String) = "leaf-$id"

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

    /** A group with several leaves — the Merlin shape task-164 exists for. */
    private fun groupOf(id: String, vararg efforts: Pair<String, Double>) = EstimationGroup(
        title = "Group $id",
        children = efforts.map { (childId, e) -> leaf(childId, e, e, e) },
        _logicalId = id
    )

    private fun phase(abbr: String, weeks: Double = 4.0) =
        ProjectPhase(name = "Phase $abbr", abbreviation = abbr, durationWeeks = weeks)

    /** Accompanying work: effort derives from the phase, so it is NOT scheduled. */
    private fun accompanying(id: String, hoursPerWeek: Double, phase: ProjectPhase) =
        TimeRelativeEstimationItem(
            _description = "accompanying $id",
            _minEffort = hoursPerWeek,
            _expectedEffort = hoursPerWeek,
            _maxEffort = hoursPerWeek,
            _phase = phase,
            _logicalId = "leaf-$id"
        )

    private fun phasedLeaf(id: String, effort: Double, phase: ProjectPhase) = FixedEstimationItem(
        _description = "leaf $id",
        _minEffort = effort,
        _expectedEffort = effort,
        _maxEffort = effort,
        _phase = phase,
        _logicalId = "leaf-$id"
    )

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
    fun `more workers do not shorten a dependency CHAIN`() {
        // teamFte is a worker COUNT (task-166), not a divisor: a leaf takes its
        // effortPT in days no matter how many people are on the team, and a
        // chain is dependency-bound, so extra workers buy nothing here.
        val schedule = versionOf(unit("a", 10.0), unit("b", 20.0), unit("c", 30.0))
            .schedule(listOf(dep("a", "b"), dep("b", "c")), teamFte = 2.0)

        assertEquals(60.0, schedule.projectDurationDays, delta)
        assertEquals(10.0, schedule.task(leafId("a")).durationDays, delta)
    }

    @Test
    fun `capacity is respected — independent work queues behind the workers`() {
        val three = arrayOf(unit("a", 10.0), unit("b", 10.0), unit("c", 10.0))

        // One worker: 30 person-days take 30 days.
        assertEquals(30.0, versionOf(*three).schedule(emptyList(), teamFte = 1.0).projectDurationDays, delta)
        // Three workers: all three run at once.
        assertEquals(10.0, versionOf(*three).schedule(emptyList(), teamFte = 3.0).projectDurationDays, delta)
    }

    @Test
    fun `a fractional team rounds DOWN to whole workers`() {
        val three = arrayOf(unit("a", 10.0), unit("b", 10.0), unit("c", 10.0))
        val twoAndAHalf = versionOf(*three).schedule(emptyList(), teamFte = 2.5)
        val two = versionOf(*three).schedule(emptyList(), teamFte = 2.0)

        assertEquals(two.projectDurationDays, twoAndAHalf.projectDurationDays, delta)
        assertEquals(20.0, twoAndAHalf.projectDurationDays, delta) // 2 slots: 10 + 10, then 10
    }

    @Test
    fun `no worker is ever double-booked`() {
        val slots = 2
        val schedule = versionOf(
            unit("a", 10.0), unit("b", 4.0), unit("c", 7.0), unit("d", 3.0), unit("e", 6.0)
        ).schedule(listOf(dep("a", "d")), teamFte = slots.toDouble())

        val leaves = schedule.tasks.filterNot { it.isGroup }
        // Sample every boundary: capacity can only be exceeded at a start.
        leaves.map { it.earliestStart }.distinct().forEach { t ->
            val running = leaves.count { it.earliestStart <= t + delta && it.earliestFinish > t + delta }
            assertTrue(running <= slots, "at t=$t, $running leaves ran with only $slots worker(s)")
        }
    }

    @Test
    fun `a dependency still wins over an idle worker`() {
        // Two workers, but b cannot start early: it waits for a.
        val schedule = versionOf(unit("a", 10.0), unit("b", 5.0))
            .schedule(listOf(dep("a", "b")), teamFte = 2.0)

        assertEquals(10.0, schedule.task(leafId("b")).earliestStart, delta)
        assertEquals(15.0, schedule.projectDurationDays, delta)
    }

    @Test
    fun `scheduling is deterministic — the same inputs give the same dates`() {
        val roots = arrayOf(unit("a", 3.0), unit("b", 5.0), unit("c", 2.0), unit("d", 8.0))
        val deps = listOf(dep("a", "d"), dep("b", "c"))
        val first = versionOf(*roots).schedule(deps, teamFte = 2.0)
        val second = versionOf(*roots).schedule(deps, teamFte = 2.0)

        assertEquals(first.tasks.map { it.logicalId }, second.tasks.map { it.logicalId })
        assertEquals(first.tasks.map { it.earliestStart }, second.tasks.map { it.earliestStart })
        assertEquals(first.projectDurationDays, second.projectDurationDays, delta)
    }

    @Test
    fun `a group's milestones consume no worker`() {
        // Two 10-day leaves, two workers: they run together and finish at 10.
        // If the group's #start/#finish milestones took slots, the leaves would
        // queue behind them.
        val schedule = versionOf(groupOf("g", "a" to 10.0, "b" to 10.0))
            .schedule(emptyList(), teamFte = 2.0)

        assertEquals(10.0, schedule.projectDurationDays, delta)
        assertEquals(0.0, schedule.task(leafId("a")).earliestStart, delta)
        assertEquals(0.0, schedule.task(leafId("b")).earliestStart, delta)
    }

    @Test
    fun `two parallel branches are BOTH critical when there are workers for both`() {
        // The case a "trace one longest chain" implementation silently gets
        // wrong: it reports only one of the two branches.
        val schedule = versionOf(unit("s", 5.0), unit("x", 10.0), unit("y", 10.0))
            .schedule(listOf(dep("s", "x"), dep("s", "y")), teamFte = 2.0)

        assertEquals(15.0, schedule.projectDurationDays, delta)
        assertTrue(schedule.task("x").onCriticalPath)
        assertTrue(schedule.task("y").onCriticalPath)
        assertTrue(schedule.task("s").onCriticalPath)
    }

    @Test
    fun `the over-commitment is gone — one worker cannot deliver 25 PT in 15 days`() {
        // THE regression this task exists for. This exact shape asserted 15.0
        // before levelling: 25 person-days delivered by one person in 15 days.
        val schedule = versionOf(unit("s", 5.0), unit("x", 10.0), unit("y", 10.0))
            .schedule(listOf(dep("s", "x"), dep("s", "y")), teamFte = 1.0)

        val totalEffort = schedule.tasks.filterNot { it.isGroup }.sumOf { it.effortPT }
        assertEquals(25.0, totalEffort, delta)
        assertEquals(25.0, schedule.projectDurationDays, delta)
    }

    @Test
    fun `a diamond takes the longest path, not the sum of everything`() {
        val schedule = versionOf(unit("a", 10.0), unit("b", 20.0), unit("c", 5.0), unit("d", 10.0))
            .schedule(listOf(dep("a", "b"), dep("a", "c"), dep("b", "d"), dep("c", "d")), teamFte = 3.0)

        // With enough workers the dependency path decides: a→b→d is 40, while
        // the sum of all four is 45.
        assertEquals(40.0, schedule.projectDurationDays, delta)
        assertEquals(30.0, schedule.task("d").earliestStart, delta)
        assertTrue(schedule.task("a").onCriticalPath)
        assertTrue(schedule.task("b").onCriticalPath)
        assertTrue(schedule.task("d").onCriticalPath)
        assertFalse(schedule.task("c").onCriticalPath, "c has 15 days of slack")
    }

    @Test
    fun `with ONE worker the diamond costs its total effort, not its longest path`() {
        // Nothing is ever idle here, so the makespan is the whole 45 PT — the
        // team, not the dependency graph, is the constraint. That is a critical
        // CHAIN rather than a critical path.
        val schedule = versionOf(unit("a", 10.0), unit("b", 20.0), unit("c", 5.0), unit("d", 10.0))
            .schedule(listOf(dep("a", "b"), dep("a", "c"), dep("b", "d"), dep("c", "d")), teamFte = 1.0)

        assertEquals(45.0, schedule.projectDurationDays, delta)
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
    fun `the band does NOT scale with team size`() {
        // task-166: a leaf takes effortPT days, so the band is already in days
        // and no longer divides by teamFte. It describes ESTIMATE uncertainty
        // along the dependency path — adding people does not make an estimate
        // more certain.
        val version = versionOf(spreadUnit("a", 0.0, 3.0, 6.0), spreadUnit("b", 0.0, 6.0, 12.0))
        val deps = listOf(dep("a", "b"))

        val solo = version.schedule(deps, teamFte = 1.0)
        val crowd = version.schedule(deps, teamFte = 3.0)

        assertEquals(9.0, solo.expectedDurationDays, delta)
        assertEquals(sqrt(5.0), solo.durationStdDevDays, delta)
        assertEquals(solo.expectedDurationDays, crowd.expectedDurationDays, delta)
        assertEquals(solo.durationStdDevDays, crowd.durationStdDevDays, delta)
    }

    @Test
    fun `parallel critical branches do not sum their variances`() {
        // Equal means (3.0) so both are critical; different variances (1 and
        // 4/9) so the tie-break is observable.
        val schedule = versionOf(spreadUnit("p", 0.0, 3.0, 6.0), spreadUnit("q", 1.0, 3.0, 5.0))
            .schedule(emptyList(), teamFte = 2.0)

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

    // ---------------------------------------------------- task-164: hierarchy

    @Test
    fun `every node is scheduled, not only the roots — the Merlin shape`() {
        // ONE root group with three children, edges drawn between the CHILDREN.
        // Under the roots-only rule this had a single unit and no schedule at all.
        val schedule = versionOf(groupOf("g", "a" to 10.0, "b" to 20.0, "c" to 30.0))
            .schedule(listOf(dep(leafId("a"), leafId("b")), dep(leafId("b"), leafId("c"))), teamFte = 1.0)

        assertNull(schedule.error)
        // 4 nodes: the group plus its three leaves.
        assertEquals(4, schedule.tasks.size)
        assertEquals(60.0, schedule.projectDurationDays, delta)
        assertEquals(0.0, schedule.task(leafId("a")).earliestStart, delta)
        assertEquals(30.0, schedule.task(leafId("c")).earliestStart, delta)
    }

    @Test
    fun `a group's dates ROLL UP from its children`() {
        val schedule = versionOf(groupOf("g", "a" to 10.0, "b" to 20.0))
            .schedule(listOf(dep(leafId("a"), leafId("b"))), teamFte = 1.0)

        val g = schedule.task("g")
        assertTrue(g.isGroup)
        assertEquals(0.0, g.earliestStart, delta)   // earliest child start
        assertEquals(30.0, g.earliestFinish, delta) // latest child finish
    }

    @Test
    fun `a group's durationDays is its SPAN, not an effort quotient`() {
        // Three workers, no edges: the children genuinely run together, so the
        // span is the longest child while the effort is their sum.
        val schedule = versionOf(groupOf("g", "a" to 10.0, "b" to 20.0, "c" to 30.0))
            .schedule(emptyList(), teamFte = 3.0)

        val g = schedule.task("g")
        assertEquals(30.0, g.durationDays, delta)   // span  = max(child)
        assertEquals(60.0, g.effortPT, delta)       // effort = sum(child)
        assertEquals(30.0, schedule.projectDurationDays, delta)

        // With ONE worker the same group takes its whole effort — capacity, not
        // the tree, decides. This is what levelling bought (task-166).
        val levelled = versionOf(groupOf("g", "a" to 10.0, "b" to 20.0, "c" to 30.0))
            .schedule(emptyList(), teamFte = 1.0)
        assertEquals(60.0, levelled.task("g").durationDays, delta)
        assertEquals(60.0, levelled.projectDurationDays, delta)
    }

    @Test
    fun `an edge between two GROUPS orders every leaf of one after the other`() {
        val schedule = versionOf(groupOf("g1", "a" to 10.0, "b" to 20.0), groupOf("g2", "c" to 5.0))
            .schedule(listOf(dep("g1", "g2")), teamFte = 2.0)

        assertNull(schedule.error)
        // g1 spans 0..20 (its leaves overlap); g2's leaf waits for ALL of g1.
        assertEquals(20.0, schedule.task("g1").earliestFinish, delta)
        assertEquals(20.0, schedule.task(leafId("c")).earliestStart, delta)
        assertEquals(25.0, schedule.projectDurationDays, delta)
    }

    @Test
    fun `a group is critical when only ONE of its leaves is`() {
        val schedule = versionOf(groupOf("g1", "a" to 10.0, "b" to 1.0), groupOf("g2", "c" to 5.0))
            .schedule(listOf(dep("g1", "g2")), teamFte = 2.0)

        assertTrue(schedule.task(leafId("a")).onCriticalPath, "the long leaf drives the group")
        assertFalse(schedule.task(leafId("b")).onCriticalPath, "the short leaf has slack")
        assertTrue(schedule.task("g1").onCriticalPath, "so the group is critical")
    }

    @Test
    fun `a cycle drawn between two GROUPS is reported with the GROUP ids`() {
        val schedule = versionOf(groupOf("g1", "a" to 10.0), groupOf("g2", "b" to 5.0))
            .schedule(listOf(dep("g1", "g2"), dep("g2", "g1")), teamFte = 1.0)

        assertEquals(ScheduleErrorKind.CYCLE, schedule.error?.kind)
        // The user drew group ids; they must not be told about lowered milestones
        // or leaves they never touched.
        assertEquals(listOf("g1", "g2"), schedule.error?.involvedLogicalIds)
        assertTrue(schedule.tasks.isEmpty())
    }

    @Test
    fun `depth and parentLogicalId describe a two-level nesting`() {
        val inner = EstimationGroup(
            title = "Inner",
            children = listOf(leaf("x", 4.0, 4.0, 4.0)),
            _logicalId = "inner"
        )
        val outer = EstimationGroup(title = "Outer", children = listOf(inner), _logicalId = "outer")
        val schedule = versionOf(outer).schedule(emptyList(), teamFte = 1.0)

        assertEquals(0, schedule.task("outer").depth)
        assertNull(schedule.task("outer").parentLogicalId)
        assertEquals(1, schedule.task("inner").depth)
        assertEquals("outer", schedule.task("inner").parentLogicalId)
        assertEquals(2, schedule.task(leafId("x")).depth)
        assertEquals("inner", schedule.task(leafId("x")).parentLogicalId)
    }

    @Test
    fun `tasks are emitted in TREE order, and no milestone leaks out`() {
        // task-157 and task-158 rebuild the tree from this list WITHOUT sorting,
        // so the ordering is a contract, not an implementation detail.
        val schedule = versionOf(
            groupOf("g1", "a" to 1.0, "b" to 2.0),
            groupOf("g2", "c" to 3.0)
        ).schedule(emptyList(), teamFte = 1.0)

        val seen = mutableSetOf<String>()
        var previousDepth = -1
        schedule.tasks.forEach { t ->
            t.parentLogicalId?.let { assertTrue(it in seen, "\${t.logicalId} precedes its parent") }
            assertTrue(t.depth <= previousDepth + 1, "depth jumped by more than one at \${t.logicalId}")
            // Milestones are an internal scheduling device and never reach the wire.
            assertFalse(t.logicalId.contains("#"), "milestone leaked: \${t.logicalId}")
            seen.add(t.logicalId)
            previousDepth = t.depth
        }
        assertEquals(listOf("g1", leafId("a"), leafId("b"), "g2", leafId("c")), schedule.tasks.map { it.logicalId })
    }

    // ── task-177: accompanying work is excluded from the plan ────────────────

    @Test
    fun `accompanying work is not scheduled and does not lengthen the plan`() {
        val ko = phase("KO")
        val version = versionOf(
            EstimationGroup(
                title = "Unit A",
                children = listOf(phasedLeaf("a", 10.0, ko), accompanying("pm", 8.0, ko)),
                _logicalId = "A"
            )
        )
        val schedule = version.schedule(emptyList(), teamFte = 1.0)

        // It is EXCLUDED, not zero-length: a zero-length node would still be in
        // `tasks` and in the makespan arithmetic.
        assertTrue(schedule.tasks.none { it.logicalId == "leaf-pm" })
        assertTrue(schedule.tasks.any { it.logicalId == "leaf-a" })
        assertEquals(10.0, schedule.projectDurationDays, 0.001)
    }

    @Test
    fun `an edge naming accompanying work is ignored rather than cycling`() {
        val ko = phase("KO")
        val version = versionOf(
            EstimationGroup(
                title = "Unit A",
                children = listOf(phasedLeaf("a", 10.0, ko), accompanying("pm", 8.0, ko)),
                _logicalId = "A"
            )
        )
        // Both directions, so a naive implementation would close a cycle.
        val schedule = version.schedule(
            listOf(dep("leaf-a", "leaf-pm"), dep("leaf-pm", "leaf-a")),
            teamFte = 1.0
        )

        assertNull(schedule.error)
        assertEquals(10.0, schedule.projectDurationDays, 0.001)
    }

    @Test
    fun `a phase window spans exactly that phase's scheduled leaves`() {
        val ko = phase("KO")
        val um = phase("UM")
        val version = versionOf(
            EstimationGroup(
                title = "Konzeption",
                children = listOf(phasedLeaf("k1", 5.0, ko), phasedLeaf("k2", 5.0, ko)),
                _logicalId = "K"
            ),
            EstimationGroup(
                title = "Umsetzung",
                children = listOf(phasedLeaf("u1", 10.0, um)),
                _logicalId = "U"
            )
        )
        // One worker, so K's two leaves serialise into 0..10 and U follows.
        val schedule = version.schedule(
            listOf(dep("K", "U")),
            teamFte = 1.0
        )

        val koWindow = schedule.phaseWindows.single { it.abbreviation == "KO" }
        assertEquals(0.0, koWindow.earliestStart, 0.001)
        assertEquals(10.0, koWindow.earliestFinish, 0.001)
        assertEquals(2, koWindow.scheduledLeafCount)
        // 10 working days is two weeks at WORKING_DAYS_PER_WEEK.
        assertEquals(2.0, koWindow.durationWeeks, 0.001)

        val umWindow = schedule.phaseWindows.single { it.abbreviation == "UM" }
        assertTrue(umWindow.earliestStart >= koWindow.earliestFinish - 0.001)
    }

    @Test
    fun `a phase carrying only accompanying work has no window`() {
        val ko = phase("KO")
        val pmPhase = phase("PM")
        val version = versionOf(
            EstimationGroup(
                title = "Unit A",
                children = listOf(phasedLeaf("a", 10.0, ko), accompanying("pm", 8.0, pmPhase)),
                _logicalId = "A"
            )
        )
        val schedule = version.schedule(emptyList(), teamFte = 1.0)

        // No scheduled leaf carries PM, so it gets no window at all — the caller
        // must say "nothing to derive from" rather than render zero weeks.
        assertTrue(schedule.phaseWindows.none { it.abbreviation == "PM" })
        assertEquals(1, schedule.phaseWindows.single { it.abbreviation == "KO" }.scheduledLeafCount)
    }
}
