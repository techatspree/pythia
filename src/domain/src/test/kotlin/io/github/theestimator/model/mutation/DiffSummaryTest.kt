package io.github.theestimator.model.mutation

import io.github.theestimator.method.threepoint.FixedEstimationItem
import io.github.theestimator.model.EstimationGroup
import io.github.theestimator.model.EstimationNode
import io.github.theestimator.model.EstimationVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DiffSummaryTest {

    private fun leaf(id: String, desc: String, min: Double = 1.0, exp: Double = 2.0, max: Double = 3.0) =
        FixedEstimationItem(
            _description = desc, _minEffort = min, _expectedEffort = exp, _maxEffort = max, _logicalId = id
        )

    private fun group(id: String, title: String, children: List<EstimationNode>) =
        EstimationGroup(title = title, children = children, _logicalId = id)

    private fun version(roots: List<EstimationNode> = emptyList(), notes: String = "", dailyRate: Double = 900.0) =
        EstimationVersion(versionNumber = 1, notes = notes, dailyRate = dailyRate, roots = roots)

    @Test
    fun `identical versions produce no changes`() {
        val v = version(listOf(group("g", "Frontend", listOf(leaf("a", "Login")))))
        assertEquals(emptyList<ChangeDescription>(), v.diffSummary(v))
    }

    @Test
    fun `dailyRate change is a single ParameterChanged`() {
        val d = version(dailyRate = 800.0).diffSummary(version(dailyRate = 900.0))
        assertEquals(listOf(ParameterChanged("dailyRate", "800", "900")), d)
    }

    @Test
    fun `notes change yields NotesChanged`() {
        val d = version(notes = "alt").diffSummary(version(notes = "neu"))
        assertEquals(listOf(NotesChanged("alt", "neu")), d)
    }

    @Test
    fun `adding a leaf yields NodeAdded with its path and type`() {
        val before = version(listOf(group("f", "Frontend", listOf(leaf("login", "Login")))))
        val after = version(listOf(group("f", "Frontend", listOf(leaf("login", "Login"), leaf("neu", "Neu")))))
        assertEquals(listOf(NodeAdded("Frontend/Neu", "FIXED")), before.diffSummary(after))
    }

    @Test
    fun `removing a leaf yields NodeRemoved`() {
        val before = version(listOf(group("f", "Frontend", listOf(leaf("login", "Login")))))
        val after = version(listOf(group("f", "Frontend", emptyList())))
        assertEquals(listOf(NodeRemoved("Frontend/Login", "FIXED")), before.diffSummary(after))
    }

    @Test
    fun `adding a group yields NodeAdded of type GROUP`() {
        val d = version().diffSummary(version(listOf(group("g", "Neue Gruppe", emptyList()))))
        assertEquals(listOf(NodeAdded("Neue Gruppe", "GROUP")), d)
    }

    @Test
    fun `renaming a leaf yields NodeRenamed keyed by logicalId`() {
        val before = version(listOf(group("f", "Frontend", listOf(leaf("x", "Login")))))
        val after = version(listOf(group("f", "Frontend", listOf(leaf("x", "Anmeldung")))))
        assertEquals(listOf(NodeRenamed("Frontend/Login", "Login", "Anmeldung")), before.diffSummary(after))
    }

    @Test
    fun `moving a leaf across groups yields NodeMoved`() {
        val before = version(listOf(group("f", "Frontend", listOf(leaf("x", "X"))), group("b", "Backend", emptyList())))
        val after = version(listOf(group("f", "Frontend", emptyList()), group("b", "Backend", listOf(leaf("x", "X")))))
        assertEquals(listOf(NodeMoved("Frontend/X", "Backend/X")), before.diffSummary(after))
    }

    @Test
    fun `changing one PT value yields a single NodeValueChanged`() {
        val before = version(listOf(group("f", "Frontend", listOf(leaf("x", "X", min = 1.0)))))
        val after = version(listOf(group("f", "Frontend", listOf(leaf("x", "X", min = 3.0)))))
        assertEquals(listOf(NodeValueChanged("Frontend/X", "optimistic", "1", "3")), before.diffSummary(after))
    }

    @Test
    fun `more than 25 changes are capped with a Truncated marker`() {
        val leaves = (1..30).map { leaf("n$it", "Item $it") }
        val before = version(listOf(group("g", "G", leaves)))
        val after = version(listOf(group("g", "G", emptyList())))
        val d = before.diffSummary(after)
        assertEquals(25, d.size)
        assertEquals(Truncated(30 - 24), d.last())
    }

    @Test
    fun `a whole-tree replace produces a bounded list`() {
        val before = version(listOf(group("a", "A", listOf(leaf("l1", "One"), leaf("l2", "Two")))))
        val after = version(listOf(group("b", "B", listOf(leaf("l3", "Three")))))
        val d = before.diffSummary(after)
        assertTrue(d.size <= 25, "expected at most 25, was ${d.size}")
        assertTrue(d.isNotEmpty())
    }
}
