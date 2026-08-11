package io.pythia.service

import io.pythia.domain.Estimation
import io.pythia.domain.draft.DraftEstimationNode
import io.pythia.domain.draft.DraftFixedItemNode
import io.pythia.domain.draft.DraftGroupNode
import io.pythia.domain.submitted.SubmittedEstimationNode
import io.pythia.domain.submitted.SubmittedEstimationVersion
import io.pythia.domain.submitted.SubmittedFixedItemNode
import io.pythia.domain.submitted.SubmittedGroupNode
import io.pythia.method.EstimationMethod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.util.Properties

class MerlinExporterTest {

    private val importer = MerlinImporter()
    private val exporter = MerlinExporter()

    private fun sampleBytes(): ByteArray =
        javaClass.getResourceAsStream("/merlin/state.sql")?.readBytes()
            ?: error("test resource /merlin/state.sql missing")

    // The exporter consumes a SubmittedEstimationVersion (the same shape the
    // xlsx/csv exporters take); build one from the imported WBS and stamp the
    // requested offerPT on every leaf.
    private fun versionFromSample(offerPTPerLeaf: Double): SubmittedEstimationVersion {
        val estimation = Estimation().apply { method = EstimationMethod.THREE_POINT_PERT }
        val draft = importer.import(sampleBytes().inputStream(), estimation, versionNumber = 1)
        val version = SubmittedEstimationVersion().apply {
            this.estimation = estimation
            this.versionNumber = 1
        }
        draft.roots.forEachIndexed { idx, node ->
            val converted = convert(node, offerPTPerLeaf)
            converted.position = idx
            version.roots.add(converted)
        }
        return version
    }

    private fun convert(node: DraftEstimationNode, offerPT: Double): SubmittedEstimationNode = when (node) {
        is DraftGroupNode -> SubmittedGroupNode().apply {
            this.title = node.title
            node.children.forEachIndexed { idx, child ->
                val converted = convert(child, offerPT)
                converted.position = idx
                converted.parent = this
                this.children.add(converted)
            }
        }
        else -> SubmittedFixedItemNode().apply {
            this.description = (node as DraftFixedItemNode).description
            this.offerPT = offerPT
        }
    }

    private fun <T> withSqlite(bytes: ByteArray, block: (java.sql.Connection) -> T): T {
        val temp = Files.createTempFile("merlin-test-", ".sqlite")
        try {
            Files.write(temp, bytes)
            return org.sqlite.JDBC()
                .connect("jdbc:sqlite:${temp.toAbsolutePath()}", Properties())
                .use(block)
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    private fun leafDescriptions(node: DraftEstimationNode): List<DraftFixedItemNode> = when (node) {
        is DraftGroupNode -> node.children.flatMap { leafDescriptions(it) }
        is DraftFixedItemNode -> listOf(node)
        else -> emptyList()
    }

    @Test
    fun `export writes offerPT into the matching leaf activity and bumps Z_OPT`() {
        val version = versionFromSample(offerPTPerLeaf = 2.5)
        val optBefore = withSqlite(sampleBytes()) { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("SELECT Z_OPT FROM ZSCHEDULEITEM WHERE ZTITLE = 'Setup TRAIN Infrastructure'")
                    .use { rs -> rs.next(); rs.getInt(1) }
            }
        }

        val exported = exporter.export(sampleBytes(), version, overwriteStructure = false)

        withSqlite(exported) { conn ->
            conn.createStatement().use { st ->
                st.executeQuery(
                    "SELECT CAST(ZGIVENWORK_ AS TEXT), Z_OPT FROM ZSCHEDULEITEM " +
                        "WHERE ZTITLE = 'Setup TRAIN Infrastructure'"
                ).use { rs ->
                    assertTrue(rs.next())
                    assertEquals(2.5, importer.parseWorkToPersonDays(rs.getString(1)))
                    assertTrue(rs.getInt(2) > optBefore, "Z_OPT must be bumped on a written row")
                }
            }
        }
    }

    @Test
    fun `a leaf with no effort clears the work column`() {
        val version = versionFromSample(offerPTPerLeaf = 0.0)

        val exported = exporter.export(sampleBytes(), version, overwriteStructure = false)

        withSqlite(exported) { conn ->
            conn.createStatement().use { st ->
                st.executeQuery(
                    "SELECT ZGIVENWORK_ FROM ZSCHEDULEITEM WHERE ZTITLE = 'Setup TRAIN Infrastructure'"
                ).use { rs ->
                    assertTrue(rs.next())
                    assertNull(rs.getBytes(1))
                }
            }
        }
    }

    @Test
    fun `diff reports inSync for an untouched document`() {
        val version = versionFromSample(offerPTPerLeaf = 1.0)

        val diff = exporter.diff(sampleBytes(), version)

        assertTrue(diff.inSync, "expected no drift, was $diff")
    }

    @Test
    fun `a renamed node is reported as drift and refused without overwriteStructure`() {
        val version = versionFromSample(offerPTPerLeaf = 1.0)
        val renamed = firstLeaf(version)
        val originalTitle = renamed.description
        renamed.description = "Renamed by the estimator"

        val diff = exporter.diff(sampleBytes(), version)
        assertFalse(diff.inSync)
        assertTrue(
            diff.missingInMerlin.any { it.endsWith("Renamed by the estimator") },
            "the new name must be missing in Merlin, was ${diff.missingInMerlin}"
        )
        assertTrue(
            diff.missingInEstimation.any { it.endsWith(originalTitle!!) },
            "the old name must be missing in the estimation, was ${diff.missingInEstimation}"
        )

        assertThrows<MerlinStructureChangedException> {
            exporter.export(sampleBytes(), version, overwriteStructure = false)
        }
    }

    @Test
    fun `overwriteStructure rewrites the Merlin WBS to match the estimation`() {
        val version = versionFromSample(offerPTPerLeaf = 1.0)
        val renamed = firstLeaf(version)
        val originalTitle = renamed.description!!
        renamed.description = "Renamed by the estimator"

        val maxBefore = primaryKeyMax(sampleBytes())
        val exported = exporter.export(sampleBytes(), version, overwriteStructure = true)

        withSqlite(exported) { conn ->
            val titles = mutableListOf<String>()
            conn.createStatement().use { st ->
                st.executeQuery("SELECT ZTITLE FROM ZSCHEDULEITEM WHERE ZTITLE IS NOT NULL").use { rs ->
                    while (rs.next()) titles.add(rs.getString(1))
                }
            }
            assertTrue(titles.contains("Renamed by the estimator"), "renamed activity must exist")
            assertFalse(titles.contains(originalTitle), "the old activity must be gone")
        }
        assertTrue(
            primaryKeyMax(exported) > maxBefore,
            "Z_PRIMARYKEY.Z_MAX for Z_ENT=48 must advance when a row is inserted"
        )
        // Re-exporting the produced document is now a no-op diff.
        val after = exporter.diff(exported, version)
        assertTrue(after.inSync, "still drifted after overwrite: $after")
    }

    @Test
    fun `exported document round-trips back through the importer with the written efforts`() {
        val version = versionFromSample(offerPTPerLeaf = 3.5)

        val exported = exporter.export(sampleBytes(), version, overwriteStructure = false)

        val reimported = importer.import(
            exported.inputStream(),
            Estimation().apply { method = EstimationMethod.THREE_POINT_PERT },
            versionNumber = 2
        )
        val leaves = reimported.roots.flatMap { leafDescriptions(it) }
        assertTrue(leaves.isNotEmpty())
        leaves.forEach { leaf ->
            assertEquals(3.5, leaf.expectedEffort, "re-imported effort for ${leaf.description}")
        }
    }

    @Test
    fun `work formatting keeps whole days integral and decimals dot-separated`() {
        assertEquals("4", exporter.formatDays(4.0))
        assertEquals("3.5", exporter.formatDays(3.5))
        assertEquals("0.25", exporter.formatDays(0.25))
        assertNotNull(exporter.encodeWork(1.0))
        assertNull(exporter.encodeWork(0.0))
        // The trailing 0x3F marker byte Merlin itself writes.
        assertEquals(0x3F.toByte(), exporter.encodeWork(1.0)!!.last())
    }

    private fun firstLeaf(version: SubmittedEstimationVersion): SubmittedEstimationNode {
        fun find(nodes: List<SubmittedEstimationNode>): SubmittedEstimationNode? {
            nodes.forEach { node ->
                if (node !is SubmittedGroupNode) return node
                find(node.children)?.let { return it }
            }
            return null
        }
        return find(version.roots) ?: error("no leaf in the imported version")
    }

    private fun primaryKeyMax(bytes: ByteArray): Long = withSqlite(bytes) { conn ->
        conn.createStatement().use { st ->
            st.executeQuery("SELECT Z_MAX FROM Z_PRIMARYKEY WHERE Z_ENT = 48").use { rs ->
                rs.next()
                rs.getLong(1)
            }
        }
    }
}
