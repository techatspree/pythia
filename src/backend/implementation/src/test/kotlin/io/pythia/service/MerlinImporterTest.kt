package io.pythia.service

import io.pythia.domain.Estimation
import io.pythia.domain.draft.DraftBucketedItemNode
import io.pythia.domain.draft.DraftEstimationNode
import io.pythia.domain.draft.DraftFixedItemNode
import io.pythia.domain.draft.DraftGroupNode
import io.pythia.method.EstimationMethod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MerlinImporterTest {

    private val importer = MerlinImporter()

    private fun sampleStream() =
        javaClass.getResourceAsStream("/merlin/state.sql") ?: error("test resource /merlin/state.sql missing")

    private fun leaves(node: DraftEstimationNode): List<DraftFixedItemNode> = when (node) {
        is DraftGroupNode -> node.children.flatMap { leaves(it) }
        is DraftFixedItemNode -> listOf(node)
        else -> emptyList()
    }

    private fun bucketedLeaves(node: DraftEstimationNode): List<DraftBucketedItemNode> = when (node) {
        is DraftGroupNode -> node.children.flatMap { bucketedLeaves(it) }
        is DraftBucketedItemNode -> listOf(node)
        else -> emptyList()
    }

    @Test
    fun `three-point import builds the WBS tree with single work seeded into the triple`() {
        val estimation = Estimation().apply { method = EstimationMethod.THREE_POINT_PERT }

        val version = importer.import(sampleStream(), estimation, versionNumber = 1)

        val rootTitles = version.roots.filterIsInstance<DraftGroupNode>().map { it.title }
        assertTrue(
            rootTitles.containsAll(listOf("Initialisation", "Implementation", "Closure")),
            "top-level WBS groups expected, was $rootTitles"
        )
        val setup = version.roots.flatMap { leaves(it) }.first { it.description == "Setup Project Management" }
        assertEquals(1.0, setup.minEffort)
        assertEquals(1.0, setup.expectedEffort)
        assertEquals(1.0, setup.maxEffort)
    }

    @Test
    fun `bucket import preserves the WBS tree with bucketed leaves in one Imported bucket`() {
        val estimation = Estimation().apply { method = EstimationMethod.BUCKET_SAMPLED_PERT }

        val version = importer.import(sampleStream(), estimation, versionNumber = 1)

        // One bucket, and the SAME tree shape as three-point (groups preserved).
        assertEquals(1, estimation.buckets.size)
        assertEquals("Imported", estimation.buckets.first().label)
        val rootTitles = version.roots.filterIsInstance<DraftGroupNode>().map { it.title }
        assertTrue(
            rootTitles.containsAll(listOf("Initialisation", "Implementation", "Closure")),
            "top-level WBS groups expected (tree preserved), was $rootTitles"
        )
        // Leaves are bucketed items assigned to the imported bucket; a sample carries work.
        val bucketed = version.roots.flatMap { bucketedLeaves(it) }
        assertTrue(bucketed.isNotEmpty(), "expected bucketed leaves nested in the tree")
        assertTrue(bucketed.all { it.bucket === estimation.buckets.first() }, "all leaves use the imported bucket")
        val sample = bucketed.first { it.isSample == true }
        assertEquals(1.0, sample.minEffort)
    }

    @Test
    fun `parseWorkToPersonDays converts day week hour and blanks`() {
        assertEquals(1.0, importer.parseWorkToPersonDays("1d?"))
        assertEquals(5.0, importer.parseWorkToPersonDays("1w"))
        assertEquals(0.5, importer.parseWorkToPersonDays("4h"))
        assertEquals(0.0, importer.parseWorkToPersonDays(null))
        assertEquals(0.0, importer.parseWorkToPersonDays("  "))
    }
}
