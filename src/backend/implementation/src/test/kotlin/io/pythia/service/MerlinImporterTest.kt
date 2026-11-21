package io.pythia.service

import io.pythia.domain.Estimation
import io.pythia.domain.draft.DraftBucketedItemNode
import io.pythia.domain.draft.DraftEstimationNode
import io.pythia.domain.draft.DraftEstimationVersion
import io.pythia.domain.draft.DraftFixedItemNode
import io.pythia.domain.draft.DraftGroupNode
import io.pythia.method.EstimationMethod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.util.Properties

class MerlinImporterTest {

    private val importer = MerlinImporter()

    private fun sampleStream() =
        javaClass.getResourceAsStream("/merlin/state.sql") ?: error("test resource /merlin/state.sql missing")

    private fun sampleBytes(): ByteArray = sampleStream().use { it.readBytes() }

    // A copy of the sample with [sql] applied. The connection is CLOSED before
    // the bytes are read back, so every change is in the file.
    private fun sampleWith(vararg sql: String): ByteArray {
        val temp = Files.createTempFile("merlin-import-test-", ".sqlite")
        try {
            Files.write(temp, sampleBytes())
            org.sqlite.JDBC().connect("jdbc:sqlite:${temp.toAbsolutePath()}", Properties()).use { conn ->
                conn.createStatement().use { st -> sql.forEach { st.executeUpdate(it) } }
            }
            return Files.readAllBytes(temp)
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    private fun importBytes(bytes: ByteArray, method: EstimationMethod) =
        importer.import(bytes.inputStream(), Estimation().apply { this.method = method }, versionNumber = 1)

    private fun allNodes(node: DraftEstimationNode): List<DraftEstimationNode> = when (node) {
        is DraftGroupNode -> listOf(node) + node.children.flatMap { allNodes(it) }
        else -> listOf(node)
    }

    private fun title(node: DraftEstimationNode): String? = when (node) {
        is DraftGroupNode -> node.title
        is DraftFixedItemNode -> node.description
        is DraftBucketedItemNode -> node.description
        else -> error("unexpected node ${node::class.simpleName}")
    }

    // Each edge's ids resolved back to the titles of the nodes they name.
    private fun edgeTitles(version: DraftEstimationVersion): List<Pair<String?, String?>> {
        val byId = version.roots.flatMap { allNodes(it) }.associateBy { it.logicalId.toString() }
        return version.scheduleDependencies.map { dep ->
            assertSame(version, dep.version)
            title(byId.getValue(dep.fromLogicalId)) to title(byId.getValue(dep.toLogicalId))
        }
    }

    // In Merlin row order (Z_PK): row 1 is Implementation → Closure, row 2 Initialisation → Implementation.
    private val demoEdges = listOf("Implementation" to "Closure", "Initialisation" to "Implementation")

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

    @Test
    fun `imports the demo file's two dependencies between top-level groups`() {
        val version = importBytes(sampleBytes(), EstimationMethod.THREE_POINT_PERT)

        assertEquals(demoEdges, edgeTitles(version))
    }

    @Test
    fun `the bucket method imports the same dependencies`() {
        val version = importBytes(sampleBytes(), EstimationMethod.BUCKET_SAMPLED_PERT)

        assertEquals(demoEdges, edgeTitles(version))
    }

    @Test
    fun `unsupported types, unknown endpoints, self edges and duplicates are skipped`() {
        // pk 4 Initialisation, 6 Implementation, 9 Closure (top-level groups);
        // pk 61 "Setup Project Management" and 106 "Cluster Setup" are leaves.
        val bytes = sampleWith(
            // start-to-start (or any non-zero type): no Pythia equivalent
            "INSERT INTO ZDEPENDENCY (ZPREVIOUSACTIVITY_, ZNEXTACTIVITY_, ZTYPE) VALUES (4, 9, 1)",
            // dangling successor
            "INSERT INTO ZDEPENDENCY (ZPREVIOUSACTIVITY_, ZNEXTACTIVITY_, ZTYPE) VALUES (4, 999999, 0)",
            // self-edge
            "INSERT INTO ZDEPENDENCY (ZPREVIOUSACTIVITY_, ZNEXTACTIVITY_, ZTYPE) VALUES (6, 6, 0)",
            // exact duplicate of Z_PK 1 (Implementation → Closure)
            "INSERT INTO ZDEPENDENCY (ZPREVIOUSACTIVITY_, ZNEXTACTIVITY_, ZTYPE) VALUES (6, 9, 0)",
            // a lagged finish-to-start edge between two leaves: kept, lag dropped
            "INSERT INTO ZDEPENDENCY (ZPREVIOUSACTIVITY_, ZNEXTACTIVITY_, ZTYPE, ZLAG_) VALUES (61, 106, 0, X'01')"
        )

        val version = importBytes(bytes, EstimationMethod.THREE_POINT_PERT)

        assertEquals(demoEdges + ("Setup Project Management" to "Cluster Setup"), edgeTitles(version))
    }

    @Test
    fun `a document without a ZDEPENDENCY table imports with no edges`() {
        val untouched = importBytes(sampleBytes(), EstimationMethod.THREE_POINT_PERT)

        val version = importBytes(sampleWith("DROP TABLE ZDEPENDENCY"), EstimationMethod.THREE_POINT_PERT)

        assertEquals(untouched.roots.size, version.roots.size)
        assertEquals(untouched.roots.map { title(it) }, version.roots.map { title(it) })
        assertTrue(version.scheduleDependencies.isEmpty(), "expected no edges, was ${version.scheduleDependencies.size}")
    }
}
