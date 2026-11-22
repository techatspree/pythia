package io.pythia.service

import io.pythia.domain.Estimation
import io.pythia.domain.draft.DraftEstimationNode
import io.pythia.domain.draft.DraftFixedItemNode
import io.pythia.domain.draft.DraftGroupNode
import io.pythia.domain.submitted.SubmittedEstimationNode
import io.pythia.domain.submitted.SubmittedEstimationVersion
import io.pythia.domain.submitted.SubmittedFixedItemNode
import io.pythia.domain.submitted.SubmittedGroupNode
import io.pythia.domain.submitted.SubmittedScheduleDependency
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

class MerlinMergerTest {

    private val importer = MerlinImporter()
    private val merger = MerlinMerger(importer)

    private fun sampleBytes(): ByteArray =
        javaClass.getResourceAsStream("/merlin/state.sql")?.readBytes()
            ?: error("test resource /merlin/state.sql missing")

    // The merger consumes a SubmittedEstimationVersion (the same shape the
    // xlsx/csv exporters take); build one from the imported WBS and stamp the
    // requested offerPT on every leaf.
    private fun versionFromSample(offerPTPerLeaf: Double): SubmittedEstimationVersion =
        versionFromSample { offerPTPerLeaf }

    // Per-leaf variant: [offerPT] decides each imported leaf's value.
    private fun versionFromSample(offerPT: (DraftFixedItemNode) -> Double): SubmittedEstimationVersion {
        val estimation = Estimation().apply { method = EstimationMethod.THREE_POINT_PERT }
        val draft = importer.import(sampleBytes().inputStream(), estimation, versionNumber = 1)
        val version = SubmittedEstimationVersion().apply {
            this.estimation = estimation
            this.versionNumber = 1
        }
        draft.roots.forEachIndexed { idx, node ->
            val converted = convert(node, offerPT)
            converted.position = idx
            version.roots.add(converted)
        }
        return version
    }

    private fun convert(node: DraftEstimationNode, offerPT: (DraftFixedItemNode) -> Double): SubmittedEstimationNode =
        convertNode(node, offerPT).also { it.logicalId = node.logicalId }

    private fun convertNode(node: DraftEstimationNode, offerPT: (DraftFixedItemNode) -> Double): SubmittedEstimationNode = when (node) {
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
            this.offerPT = offerPT(node)
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

        val exported = merger.merge(sampleBytes(), version, overwriteStructure = false).document

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

        val exported = merger.merge(sampleBytes(), version, overwriteStructure = false).document

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

        val diff = merger.diff(sampleBytes(), version)

        assertTrue(diff.inSync, "expected no drift, was $diff")
    }

    @Test
    fun `a renamed node is reported as drift and refused without overwriteStructure`() {
        val version = versionFromSample(offerPTPerLeaf = 1.0)
        val renamed = firstLeaf(version)
        val originalTitle = renamed.description
        renamed.description = "Renamed by the estimator"

        val diff = merger.diff(sampleBytes(), version)
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
            merger.merge(sampleBytes(), version, overwriteStructure = false).document
        }
    }

    @Test
    fun `overwriteStructure rewrites the Merlin WBS to match the estimation`() {
        val version = versionFromSample(offerPTPerLeaf = 1.0)
        val renamed = firstLeaf(version)
        val originalTitle = renamed.description!!
        renamed.description = "Renamed by the estimator"

        val maxBefore = primaryKeyMax(sampleBytes())
        val exported = merger.merge(sampleBytes(), version, overwriteStructure = true).document

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
        val after = merger.diff(exported, version)
        assertTrue(after.inSync, "still drifted after overwrite: $after")
    }

    @Test
    fun `exported document round-trips back through the importer with the written efforts`() {
        val version = versionFromSample(offerPTPerLeaf = 3.5)

        val exported = merger.merge(sampleBytes(), version, overwriteStructure = false).document

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
        assertEquals("4", merger.formatDays(4.0))
        assertEquals("3.5", merger.formatDays(3.5))
        assertEquals("0.25", merger.formatDays(0.25))
        assertNotNull(merger.encodeWork(1.0))
        assertNull(merger.encodeWork(0.0))
        // The trailing 0x3F marker byte Merlin itself writes.
        assertEquals(0x3F.toByte(), merger.encodeWork(1.0)!!.last())
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
        val temp = Files.createTempFile("merlin-merge-test-", ".sqlite")
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

        val exported = merger.merge(sampleBytes(), versionWithImplementationRenamed(), overwriteStructure = true).document

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

        val exported = merger.merge(bytes, versionWithImplementationRenamed(), overwriteStructure = true).document

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

        val exported = merger.merge(sampleBytes(), version, overwriteStructure = true).document

        assertEquals(2, rowCount(exported, "SELECT count(*) FROM ZDEPENDENCY"))
        assertEquals(emptyMap<String, Int>(), allZero(danglingReferences(exported)))
    }

    // ── task-223: effort only where it changes, dependencies add-only, report ─

    private fun root(version: SubmittedEstimationVersion, title: String): SubmittedGroupNode =
        version.roots.filterIsInstance<SubmittedGroupNode>().first { it.title == title }

    // Adds Pythia edges between the given nodes (by their logicalIds).
    private fun SubmittedEstimationVersion.withEdges(
        vararg edges: Pair<SubmittedEstimationNode, SubmittedEstimationNode>
    ): SubmittedEstimationVersion = also { version ->
        edges.forEach { (from, to) ->
            version.scheduleDependencies.add(SubmittedScheduleDependency().apply {
                fromLogicalId = from.logicalId.toString()
                toLogicalId = to.logicalId.toString()
                this.version = version
            })
        }
    }

    private fun dependencyRows(bytes: ByteArray): List<Pair<Long, Long>> = withSqlite(bytes) { conn ->
        conn.createStatement().use { st ->
            st.executeQuery("SELECT ZPREVIOUSACTIVITY_, ZNEXTACTIVITY_ FROM ZDEPENDENCY ORDER BY Z_PK").use { rs ->
                buildList { while (rs.next()) add(rs.getLong(1) to rs.getLong(2)) }
            }
        }
    }

    private fun optByPk(bytes: ByteArray): Map<Long, Int> = withSqlite(bytes) { conn ->
        conn.createStatement().use { st ->
            st.executeQuery("SELECT Z_PK, Z_OPT FROM ZSCHEDULEITEM WHERE ZTITLE IS NOT NULL").use { rs ->
                buildMap { while (rs.next()) put(rs.getLong(1), rs.getInt(2)) }
            }
        }
    }

    private fun isEmpty(report: MerlinMergeReport) = report == MerlinMergeReport(
        emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
        MerlinRemovedContent(0, 0, 0, 0, 0)
    )

    @Test
    fun `an unchanged leaf is not rewritten`() {
        // Each leaf gets exactly its own work from the sample ("1d" → 1.0, none → 0.0).
        val version = versionFromSample { leaf -> leaf.expectedEffort ?: 0.0 }

        val result = merger.merge(sampleBytes(), version, overwriteStructure = false)

        assertEquals(emptyList<MerlinEffortChange>(), result.report.effortChanges)
        assertEquals(optByPk(sampleBytes()), optByPk(result.document), "no activity's Z_OPT may change")
    }

    @Test
    fun `merging the merged document again changes nothing`() {
        // 1.0833 is stored as "1.08d": the second merge must compare it as equal.
        val version = versionFromSample(offerPTPerLeaf = 1.0833)
        val first = merger.merge(sampleBytes(), version, overwriteStructure = false)
        assertTrue(first.report.effortChanges.isNotEmpty())

        val second = merger.merge(first.document, version, overwriteStructure = false)

        assertTrue(isEmpty(second.report), "second merge must report nothing, was ${second.report}")
    }

    @Test
    fun `a Pythia edge Merlin lacks is added as finish-to-start`() {
        val version = versionFromSample(offerPTPerLeaf = 1.0)
        version.withEdges(root(version, "Initialisation") to root(version, "Closure"))

        val result = merger.merge(sampleBytes(), version, overwriteStructure = false)

        assertEquals(listOf(6L to 9L, 4L to 6L, 4L to 9L), dependencyRows(result.document))
        withSqlite(result.document) { conn ->
            assertEquals(0, count(conn, "SELECT ZTYPE FROM ZDEPENDENCY WHERE Z_PK = 5"))
            assertEquals(1, count(conn, "SELECT count(*) FROM ZDEPENDENCY WHERE Z_PK = 5 AND ZLAG_ IS NULL"))
            assertEquals(5, count(conn, "SELECT Z_MAX FROM Z_PRIMARYKEY WHERE Z_NAME = 'Dependency'"))
        }
        assertEquals(listOf(MerlinMergedDependency("Initialisation", "Closure")), result.report.dependenciesAdded)
        assertEquals(emptyList<MerlinSkippedDependency>(), result.report.dependenciesSkipped)
    }

    @Test
    fun `an edge Merlin already has is not duplicated`() {
        val version = versionFromSample(offerPTPerLeaf = 1.0)
        version.withEdges(root(version, "Initialisation") to root(version, "Implementation"))

        val result = merger.merge(sampleBytes(), version, overwriteStructure = false)

        assertEquals(2, dependencyRows(result.document).size)
        assertEquals(emptyList<MerlinMergedDependency>(), result.report.dependenciesAdded)
        assertEquals(emptyList<MerlinSkippedDependency>(), result.report.dependenciesSkipped)
    }

    @Test
    fun `a Merlin-only dependency is never deleted`() {
        val version = versionFromSample(offerPTPerLeaf = 1.0)

        val result = merger.merge(sampleBytes(), version, overwriteStructure = false)

        assertEquals(listOf(6L to 9L, 4L to 6L), dependencyRows(result.document))
    }

    @Test
    fun `an edge that closes a cycle is skipped and reported`() {
        // Merlin has Initialisation → Implementation → Closure.
        val version = versionFromSample(offerPTPerLeaf = 1.0)
        version.withEdges(root(version, "Closure") to root(version, "Initialisation"))

        val result = merger.merge(sampleBytes(), version, overwriteStructure = false)

        assertEquals(2, dependencyRows(result.document).size)
        assertEquals(
            listOf(MerlinSkippedDependency("Closure", "Initialisation", MerlinDependencySkipReason.WOULD_CREATE_CYCLE)),
            result.report.dependenciesSkipped
        )
    }

    @Test
    fun `an edge between a group and its own child is skipped`() {
        val version = versionFromSample(offerPTPerLeaf = 1.0)
        val implementation = root(version, "Implementation")
        val child = implementation.children.minBy { it.position }
        version.withEdges(implementation to child)

        val result = merger.merge(sampleBytes(), version, overwriteStructure = false)

        assertEquals(2, dependencyRows(result.document).size)
        assertEquals(
            listOf(MerlinDependencySkipReason.PARENT_CHILD),
            result.report.dependenciesSkipped.map { it.reason }
        )
    }

    @Test
    fun `overwrite re-adds the edges of a renamed group`() {
        val version = versionWithImplementationRenamed()
        val renamed = root(version, "Implementierung")
        version.withEdges(
            root(version, "Initialisation") to renamed,
            renamed to root(version, "Closure")
        )

        val result = merger.merge(sampleBytes(), version, overwriteStructure = true)
        val report = result.report

        assertEquals(
            listOf(
                MerlinMergedDependency("Implementation", "Closure"),
                MerlinMergedDependency("Initialisation", "Implementation")
            ),
            report.dependenciesRemoved
        )
        assertEquals(
            listOf(
                MerlinMergedDependency("Initialisation", "Implementierung"),
                MerlinMergedDependency("Implementierung", "Closure")
            ),
            report.dependenciesAdded
        )
        assertEquals(24, report.contentRemoved.assignments)
        assertEquals(31, report.activitiesRemoved.size)
        assertEquals(31, report.activitiesAdded.size)
        assertEquals(2, dependencyRows(result.document).size)
        assertEquals(emptyMap<String, Int>(), allZero(danglingReferences(result.document)))
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
