package io.pythia.service

import io.pythia.domain.submitted.SubmittedEstimationNode
import io.pythia.domain.submitted.SubmittedEstimationVersion
import io.pythia.domain.submitted.SubmittedFixedItemNode
import io.pythia.domain.submitted.SubmittedGroupNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class VersionComparisonServiceTest {

    private val service = VersionComparisonService()

    private fun leaf(id: UUID, description: String, min: Double = 1.0, exp: Double = 2.0, max: Double = 3.0): SubmittedFixedItemNode {
        return SubmittedFixedItemNode().apply {
            this.logicalId = id
            this.description = description
            this.minEffort = min
            this.expectedEffort = exp
            this.maxEffort = max
        }
    }

    private fun group(id: UUID, title: String, children: List<SubmittedEstimationNode>): SubmittedGroupNode {
        val g = SubmittedGroupNode().apply {
            this.logicalId = id
            this.title = title
        }
        children.forEachIndexed { idx, c ->
            c.parent = g
            c.position = idx
        }
        g.children.addAll(children)
        return g
    }

    private fun version(num: Int, roots: List<SubmittedEstimationNode>): SubmittedEstimationVersion {
        return SubmittedEstimationVersion().apply {
            this.versionNumber = num
            roots.forEachIndexed { idx, r ->
                r.position = idx
                this.roots.add(r)
            }
        }
    }

    @Test
    fun `(a) leaf added under an existing group appears in addedNodes with correct path`() {
        val backendId = UUID.randomUUID()
        val leafAId = UUID.randomUUID()
        val leafBId = UUID.randomUUID()

        val v1 = version(1, listOf(group(backendId, "Backend", listOf(leaf(leafAId, "A")))))
        val v2 = version(2, listOf(group(backendId, "Backend", listOf(leaf(leafAId, "A"), leaf(leafBId, "B")))))

        val diff = service.compare(v1, v2)

        assertEquals(1, diff.addedNodes.size)
        val added = diff.addedNodes.single()
        assertEquals(leafBId, added.logicalId)
        assertEquals("B", added.description)
        assertEquals(listOf("Backend"), added.path)
        assertEquals(0, diff.removedNodes.size)
        assertEquals(0, diff.modifiedNodes.size)
    }

    @Test
    fun `(b) group renamed appears in modifiedNodes with changedFields title`() {
        val backendId = UUID.randomUUID()
        val leafId = UUID.randomUUID()

        val v1 = version(1, listOf(group(backendId, "Backend", listOf(leaf(leafId, "A")))))
        val v2 = version(2, listOf(group(backendId, "Server", listOf(leaf(leafId, "A")))))

        val diff = service.compare(v1, v2)

        assertEquals(0, diff.addedNodes.size)
        assertEquals(0, diff.removedNodes.size)
        // The leaf's parent path also changed because the parent's title changed.
        // We expect exactly two modifications: the renamed group + the leaf (path changed).
        assertEquals(2, diff.modifiedNodes.size)
        val renamedGroup = diff.modifiedNodes.first { it.logicalId == backendId }
        assertEquals(listOf("title"), renamedGroup.changedFields)
        assertEquals("Backend", renamedGroup.before.title)
        assertEquals("Server", renamedGroup.after.title)

        val movedLeaf = diff.modifiedNodes.first { it.logicalId == leafId }
        assertEquals(listOf("parent"), movedLeaf.changedFields)
        assertEquals(listOf("Backend"), movedLeaf.before.path)
        assertEquals(listOf("Server"), movedLeaf.after.path)
    }

    @Test
    fun `(c) leaf moved between groups appears once in modifiedNodes and not in added or removed`() {
        val groupAId = UUID.randomUUID()
        val groupBId = UUID.randomUUID()
        val leafId = UUID.randomUUID()

        val v1 = version(1, listOf(
            group(groupAId, "GA", listOf(leaf(leafId, "Item"))),
            group(groupBId, "GB", emptyList())
        ))
        val v2 = version(2, listOf(
            group(groupAId, "GA", emptyList()),
            group(groupBId, "GB", listOf(leaf(leafId, "Item")))
        ))

        val diff = service.compare(v1, v2)

        assertTrue(diff.addedNodes.none { it.logicalId == leafId },
            "moved leaf must not appear in addedNodes")
        assertTrue(diff.removedNodes.none { it.logicalId == leafId },
            "moved leaf must not appear in removedNodes")

        val mods = diff.modifiedNodes.filter { it.logicalId == leafId }
        assertEquals(1, mods.size, "moved leaf must appear exactly once in modifiedNodes")
        val mod = mods.single()
        assertEquals(listOf("parent"), mod.changedFields)
        assertEquals(listOf("GA"), mod.before.path)
        assertEquals(listOf("GB"), mod.after.path)
    }

    @Test
    fun `(d) top-level group with two descendants deleted produces three removedNodes entries`() {
        val deletedGroupId = UUID.randomUUID()
        val leaf1Id = UUID.randomUUID()
        val leaf2Id = UUID.randomUUID()
        val keptGroupId = UUID.randomUUID()
        val keptLeafId = UUID.randomUUID()

        val v1 = version(1, listOf(
            group(deletedGroupId, "Going", listOf(leaf(leaf1Id, "One"), leaf(leaf2Id, "Two"))),
            group(keptGroupId, "Staying", listOf(leaf(keptLeafId, "Stays")))
        ))
        val v2 = version(2, listOf(
            group(keptGroupId, "Staying", listOf(leaf(keptLeafId, "Stays")))
        ))

        val diff = service.compare(v1, v2)

        assertEquals(3, diff.removedNodes.size, "the group + its two leaves must all be removed")
        val removedIds = diff.removedNodes.map { it.logicalId }.toSet()
        assertEquals(setOf(deletedGroupId, leaf1Id, leaf2Id), removedIds)
        assertEquals(0, diff.addedNodes.size)
        assertEquals(0, diff.modifiedNodes.size)
    }

    @Test
    fun `(e) middle group disappears - descendants reparent with shorter path`() {
        val rootId = UUID.randomUUID()
        val middleId = UUID.randomUUID()
        val leaf1Id = UUID.randomUUID()
        val leaf2Id = UUID.randomUUID()

        // v1: Root > Middle > {leaf1, leaf2}
        val v1 = version(1, listOf(
            group(rootId, "Root", listOf(
                group(middleId, "Middle", listOf(leaf(leaf1Id, "L1"), leaf(leaf2Id, "L2")))
            ))
        ))
        // v2: Root > {leaf1, leaf2}  — Middle removed; leaves reparented onto Root
        val v2 = version(2, listOf(
            group(rootId, "Root", listOf(leaf(leaf1Id, "L1"), leaf(leaf2Id, "L2")))
        ))

        val diff = service.compare(v1, v2)

        // The middle group is removed.
        assertEquals(1, diff.removedNodes.size)
        assertEquals(middleId, diff.removedNodes.single().logicalId)

        // The two leaves reparented — they now sit under Root (path = ["Root"])
        // instead of Root > Middle (path = ["Root", "Middle"]).
        assertEquals(0, diff.addedNodes.size)
        val movedLeaves = diff.modifiedNodes.filter { it.logicalId in setOf(leaf1Id, leaf2Id) }
        assertEquals(2, movedLeaves.size)
        for (m in movedLeaves) {
            assertEquals(listOf("parent"), m.changedFields)
            assertEquals(listOf("Root", "Middle"), m.before.path)
            assertEquals(listOf("Root"), m.after.path)
        }
    }

    @Test
    fun `comparison node DTO is shaped per type (group vs leaf)`() {
        val groupId = UUID.randomUUID()
        val leafId = UUID.randomUUID()

        val v1 = version(1, listOf(group(groupId, "G", listOf(leaf(leafId, "L")))))
        val v2 = version(2, emptyList())

        val diff = service.compare(v1, v2)
        val removedGroup = diff.removedNodes.first { it.logicalId == groupId }
        val removedLeaf = diff.removedNodes.first { it.logicalId == leafId }

        assertEquals("GROUP", removedGroup.type)
        assertEquals("G", removedGroup.title)
        assertNull(removedGroup.description)
        assertNull(removedGroup.minEffort)

        assertEquals("FIXED", removedLeaf.type)
        assertNull(removedLeaf.title)
        assertEquals("L", removedLeaf.description)
        assertEquals(1.0, removedLeaf.minEffort)
    }
}
