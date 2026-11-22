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

    // ── task-222: no dangling rows after an overwrite ────────────────────────

    // Every column of the Merlin schema that points at a ZSCHEDULEITEM,
    // ZCOMMENT or ZATTACHMENT row (measured by scanning every column of the
    // demo file). Written out HERE, independently of the production list, so a
    // reference the exporter forgets shows up as a non-zero count.
    private val referenceColumns = listOf(
        Triple("ZSCHEDULEITEM", "ZACTIVITY_", "ZSCHEDULEITEM"),
        Triple("ZSCHEDULEITEM", "ZPARENTACTIVITY_", "ZSCHEDULEITEM"),
        Triple("ZSCHEDULEITEM", "ZRANKNEXTACTIVITY_", "ZSCHEDULEITEM"),
        Triple("ZSCHEDULEITEM", "ZRANKPREVIOUSACTIVITY_", "ZSCHEDULEITEM"),
        Triple("ZDEPENDENCY", "ZPREVIOUSACTIVITY_", "ZSCHEDULEITEM"),
        Triple("ZDEPENDENCY", "ZNEXTACTIVITY_", "ZSCHEDULEITEM"),
        Triple("ZOBJECTVIEWCONFIGURATION", "ZSCHEDULEITEM_", "ZSCHEDULEITEM"),
        Triple("ZOBJECTVIEWCONFIGURATION", "ZATTACHMENT_", "ZATTACHMENT"),
        Triple("Z_48ASSOCIATEDLISTVALUES", "Z_48SCHEDULEITEMS", "ZSCHEDULEITEM"),
        Triple("Z_1ASSOCIATEDLISTVALUES", "Z_1ATTACHMENTS", "ZATTACHMENT"),
        Triple("ZCOMMENT", "ZSCHEDULEITEM", "ZSCHEDULEITEM"),
        Triple("ZCOMMENT", "ZATTACHMENT", "ZATTACHMENT"),
        Triple("ZCOMMENTREADING", "ZCOMMENT", "ZCOMMENT"),
        Triple("ZATTACHMENT", "ZSCHEDULEITEM", "ZSCHEDULEITEM"),
        Triple("ZATTACHMENT", "ZCOMMENT", "ZCOMMENT"),
        Triple("ZCOST", "ZSCHEDULEITEM", "ZSCHEDULEITEM"),
        Triple("ZCOST", "ZATTACHMENT", "ZATTACHMENT"),
        Triple("ZCUSTOMATTRIBUTEVALUE", "ZSCHEDULEITEM", "ZSCHEDULEITEM"),
        Triple("ZCUSTOMATTRIBUTEVALUE", "ZATTACHMENT", "ZATTACHMENT")
    )

    // table.column → rows whose non-NULL reference names a row that does not exist.
    private fun danglingReferences(bytes: ByteArray): Map<String, Int> = withSqlite(bytes) { conn ->
        referenceColumns.associate { (table, column, target) ->
            "$table.$column" to count(
                conn,
                "SELECT count(*) FROM $table WHERE $column IS NOT NULL AND $column NOT IN (SELECT Z_PK FROM $target)"
            )
        }
    }

    private fun count(conn: java.sql.Connection, sql: String): Int =
        conn.createStatement().use { st -> st.executeQuery(sql).use { rs -> rs.next(); rs.getInt(1) } }

    private fun rowCount(bytes: ByteArray, sql: String): Int = withSqlite(bytes) { count(it, sql) }

    // A copy of the sample with [sql] applied; the connection is closed before
    // the bytes are read back.
    private fun sampleWith(vararg sql: String): ByteArray {
        val temp = Files.createTempFile("merlin-export-test-", ".sqlite")
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

    // The demo's top-level group "Implementation" (pk 6) renamed: the overwrite
    // deletes its whole subtree (31 activities, 24 assignments).
    private fun versionWithImplementationRenamed(): SubmittedEstimationVersion =
        versionFromSample(offerPTPerLeaf = 1.0).also { version ->
            version.roots.filterIsInstance<SubmittedGroupNode>()
                .first { it.title == "Implementation" }
                .title = "Implementierung"
        }

    private fun allZero(counts: Map<String, Int>) = counts.filterValues { it != 0 }

    @Test
    fun `overwrite that deletes a group leaves no dangling references`() {
        // The helper is not vacuous: the untouched sample has none either, and
        // the old exporter left 2 dependency rows and 7 view configurations.
        assertEquals(emptyMap<String, Int>(), allZero(danglingReferences(sampleBytes())))

        val exported = exporter.export(sampleBytes(), versionWithImplementationRenamed(), overwriteStructure = true)

        assertEquals(emptyMap<String, Int>(), allZero(danglingReferences(exported)))
        // Both demo edges touch the deleted subtree (Initialisation → Implementation → Closure).
        assertEquals(0, rowCount(exported, "SELECT count(*) FROM ZDEPENDENCY"))
    }

    @Test
    fun `owned content is deleted with the activity`() {
        fun ent(name: String) = "(SELECT Z_ENT FROM Z_PRIMARYKEY WHERE Z_NAME = '$name')"
        // pk 49 "Final QA" lies inside "Implementation". 9xxx pks are free in the sample.
        val bytes = sampleWith(
            // an assignment of pk 49 (Assignment, Z_ENT 51), with a cost on it
            "INSERT INTO ZSCHEDULEITEM (Z_PK, Z_ENT, Z_OPT, ZACTIVITY_) VALUES (9010, 51, 1, 49)",
            "INSERT INTO ZCOST (Z_PK, Z_ENT, Z_OPT, ZSCHEDULEITEM) VALUES (9011, ${ent("Cost")}, 1, 9010)",
            // a comment on pk 49 and a reading of it
            "INSERT INTO ZCOMMENT (Z_PK, Z_ENT, Z_OPT, ZSCHEDULEITEM) VALUES (9001, ${ent("Comment")}, 1, 49)",
            "INSERT INTO ZCOMMENTREADING (Z_PK, Z_ENT, Z_OPT, ZCOMMENT) VALUES (9002, ${ent("CommentReading")}, 1, 9001)",
            // an attachment on pk 49 with a comment of its own
            "INSERT INTO ZATTACHMENT (Z_PK, Z_ENT, Z_OPT, ZSCHEDULEITEM) VALUES (9003, ${ent("Attachment")}, 1, 49)",
            "INSERT INTO ZCOMMENT (Z_PK, Z_ENT, Z_OPT, ZATTACHMENT) VALUES (9004, ${ent("Comment")}, 1, 9003)",
            // the circular case: an attachment hanging off the first COMMENT
            "INSERT INTO ZATTACHMENT (Z_PK, Z_ENT, Z_OPT, ZCOMMENT) VALUES (9005, ${ent("Attachment")}, 1, 9001)",
            // a cost and a custom attribute value on pk 49
            "INSERT INTO ZCOST (Z_PK, Z_ENT, Z_OPT, ZSCHEDULEITEM) VALUES (9006, ${ent("Cost")}, 1, 49)",
            "INSERT INTO ZCUSTOMATTRIBUTEVALUE (Z_PK, Z_ENT, Z_OPT, ZSCHEDULEITEM) " +
                "VALUES (9007, ${ent("CustomAttributeValue")}, 1, 49)",
            // a list-value join for pk 49
            "INSERT INTO Z_48ASSOCIATEDLISTVALUES (Z_48SCHEDULEITEMS, Z_34ASSOCIATEDLISTVALUES) VALUES (49, 1)"
        )

        val exported = exporter.export(bytes, versionWithImplementationRenamed(), overwriteStructure = true)

        val inserted = mapOf(
            "ZSCHEDULEITEM" to "Z_PK = 9010",
            "ZCOST" to "Z_PK IN (9006, 9011)",
            "ZCOMMENT" to "Z_PK IN (9001, 9004)",
            "ZCOMMENTREADING" to "Z_PK = 9002",
            "ZATTACHMENT" to "Z_PK IN (9003, 9005)",
            "ZCUSTOMATTRIBUTEVALUE" to "Z_PK = 9007",
            "Z_48ASSOCIATEDLISTVALUES" to "Z_48SCHEDULEITEMS = 49"
        )
        inserted.forEach { (table, where) ->
            assertEquals(0, rowCount(exported, "SELECT count(*) FROM $table WHERE $where"), "$table $where must be gone")
        }
        assertEquals(emptyMap<String, Int>(), allZero(danglingReferences(exported)))
    }

    @Test
    fun `kept activities keep their dependencies`() {
        // The first leaf is touched by no dependency (both demo edges join top-level groups).
        val version = versionFromSample(offerPTPerLeaf = 1.0)
        firstLeaf(version).description = "Renamed by the estimator"

        val exported = exporter.export(sampleBytes(), version, overwriteStructure = true)

        assertEquals(2, rowCount(exported, "SELECT count(*) FROM ZDEPENDENCY"))
        assertEquals(emptyMap<String, Int>(), allZero(danglingReferences(exported)))
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
