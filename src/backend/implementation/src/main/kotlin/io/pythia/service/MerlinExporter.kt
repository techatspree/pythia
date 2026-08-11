package io.pythia.service

import io.pythia.domain.submitted.SubmittedEstimationNode
import io.pythia.domain.submitted.SubmittedEstimationVersion
import io.pythia.domain.submitted.SubmittedGroupNode
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import java.nio.file.Files
import java.nio.file.Path
import java.security.SecureRandom
import java.sql.Connection
import java.util.Base64
import java.util.Properties

// Writes estimated effort back into a Merlin Project document (task-133) — the
// mirror of MerlinImporter. Only ever edits a COPY: the uploaded bytes are
// written to a temp file, modified there, and returned; the user's own document
// is never touched.
//
// The value written is the calculated offerPT (mean + risk surcharge) of each
// LEAF, taken off the resolved version — Merlin derives a parent's work from
// its children, so group activities are left alone. Correlation is by WBS path
// (the titles from the top level down), which is the only key the import left
// behind; a structural drift is therefore surfaced to the user rather than
// guessed at.
//
// Column names, entity ids and unit factors here are the physical Merlin /
// Core Data schema (MagicNumber); the reconciliation is inherently branchy
// (CyclomaticComplexMethod) and this is one cohesive exporter split into small
// helpers (TooManyFunctions).
@Suppress("MagicNumber", "CyclomaticComplexMethod", "TooManyFunctions")
@ApplicationScoped
class MerlinExporter {

    data class MerlinStructureDiff(
        val missingInMerlin: List<String>,
        val missingInEstimation: List<String>,
        val reordered: List<String>
    ) {
        val inSync: Boolean
            get() = missingInMerlin.isEmpty() && missingInEstimation.isEmpty() && reordered.isEmpty()
    }

    private data class Activity(
        val pk: Long,
        val parentPk: Long?,
        val order: Double,
        val title: String
    )

    // One estimation node flattened to its WBS path plus what to write. The
    // parent path and title are captured DURING the walk, never recovered by
    // splitting `path`: a Merlin title may itself contain the " / " separator
    // (the sample has "… Layer-2 / Layer-3"), so string surgery would resolve
    // the wrong parent.
    private data class EstimationEntry(
        val path: String,
        val parentPath: String,
        val title: String,
        val depth: Int,
        val isLeaf: Boolean,
        val offerPT: Double
    )

    fun diff(input: ByteArray, version: SubmittedEstimationVersion): MerlinStructureDiff =
        withDocument(input) { conn -> computeDiff(readActivities(conn), estimationEntries(version)) }

    fun export(
        input: ByteArray,
        version: SubmittedEstimationVersion,
        overwriteStructure: Boolean
    ): ByteArray {
        val entries = estimationEntries(version)
        Log.info(
            "Exporting estimation ${version.estimation?.id} version ${version.versionNumber} " +
                "into a Merlin copy (${entries.count { it.isLeaf }} leaves, " +
                "overwriteStructure=$overwriteStructure)"
        )
        val modified = modifyDocument(input, version, entries, overwriteStructure)
        val result = if (MerlinDocument.isZip(input)) {
            MerlinDocument.repackWithSqlite(input, modified)
        } else {
            modified
        }
        Log.info("Merlin export finished for estimation ${version.estimation?.id} (${result.size} bytes)")
        return result
    }

    // The extension a caller should give the returned document: a bundle stays
    // a zip, a bare store stays a .sql. Keeps byte-poking out of the REST layer.
    fun filenameExtension(document: ByteArray): String =
        if (MerlinDocument.isZip(document)) "zip" else "sql"

    // Broad catch on purpose: any failure in here (SQL, IO, parsing) is worth an
    // ERROR log with the estimation id before it propagates to the REST mapper.
    @Suppress("TooGenericExceptionCaught")
    private fun modifyDocument(
        input: ByteArray,
        version: SubmittedEstimationVersion,
        entries: List<EstimationEntry>,
        overwriteStructure: Boolean
    ): ByteArray {
        val sqlite = MerlinDocument.extractSqlite(input)
        val temp = Files.createTempFile("merlin-export-", ".sqlite")
        try {
            Files.write(temp, sqlite)
            openConnection(temp).use { conn -> applyToConnection(conn, version, entries, overwriteStructure) }
            return Files.readAllBytes(temp)
        } catch (e: MerlinStructureChangedException) {
            throw e
        } catch (e: Exception) {
            Log.error("Merlin export failed for estimation ${version.estimation?.id}", e)
            throw e
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    private fun applyToConnection(
        conn: Connection,
        version: SubmittedEstimationVersion,
        entries: List<EstimationEntry>,
        overwriteStructure: Boolean
    ) {
        conn.autoCommit = false
        val activities = readActivities(conn)
        val structureDiff = computeDiff(activities, entries)
        if (!structureDiff.inSync) {
            Log.warn(
                "Merlin document structure drifted from estimation ${version.estimation?.id}: " +
                    "${structureDiff.missingInMerlin.size} missing in Merlin, " +
                    "${structureDiff.missingInEstimation.size} missing in the estimation, " +
                    "${structureDiff.reordered.size} reordered"
            )
            if (!overwriteStructure) throw MerlinStructureChangedException(structureDiff)
        }
        val current = if (structureDiff.inSync) {
            activities
        } else {
            reconcileStructure(conn, activities, entries)
            readActivities(conn)
        }
        writeWork(conn, current, entries)
        conn.commit()
    }

    // ── Reading ─────────────────────────────────────────────────────────────

    private fun <T> withDocument(input: ByteArray, block: (Connection) -> T): T {
        val sqlite = MerlinDocument.extractSqlite(input)
        val temp = Files.createTempFile("merlin-read-", ".sqlite")
        try {
            Files.write(temp, sqlite)
            return openConnection(temp).use(block)
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    private fun openConnection(dbFile: Path): Connection =
        org.sqlite.JDBC().connect("jdbc:sqlite:${dbFile.toAbsolutePath()}", Properties())

    private fun readActivities(conn: Connection): List<Activity> =
        conn.createStatement().use { st ->
            st.executeQuery(MerlinImporter.ACTIVITIES_QUERY).use { rs -> mapActivities(rs) }
        }

    private fun mapActivities(rs: java.sql.ResultSet): List<Activity> {
        val activities = mutableListOf<Activity>()
        while (rs.next()) {
            val parent = rs.getLong(2).let { if (rs.wasNull()) null else it }
            activities.add(
                Activity(
                    pk = rs.getLong(1),
                    parentPk = parent,
                    order = rs.getDouble(3),
                    title = rs.getString(5).trim()
                )
            )
        }
        return activities
    }

    // ── Paths ───────────────────────────────────────────────────────────────

    // The Merlin WBS as ordered " / "-joined paths. The project-root activity
    // IS the estimation, so it contributes no segment (same convention the
    // importer uses when it maps the root's children to the estimation roots).
    private fun merlinPaths(activities: List<Activity>): Map<String, Long> {
        val root = activities.firstOrNull { it.parentPk == null } ?: return emptyMap()
        val byParent = activities.groupBy { it.parentPk }
        val paths = LinkedHashMap<String, Long>()
        fun walk(parentPk: Long, prefix: String) {
            (byParent[parentPk] ?: emptyList()).sortedBy { it.order }.forEach { act ->
                val path = if (prefix.isEmpty()) act.title else "$prefix / ${act.title}"
                paths[path] = act.pk
                walk(act.pk, path)
            }
        }
        walk(root.pk, "")
        return paths
    }

    private fun estimationEntries(version: SubmittedEstimationVersion): List<EstimationEntry> {
        val entries = mutableListOf<EstimationEntry>()
        fun walk(nodes: List<SubmittedEstimationNode>, prefix: String, depth: Int) {
            nodes.sortedBy { it.position }.forEach { node ->
                val segment = if (node is SubmittedGroupNode) node.title ?: "" else node.description ?: ""
                val path = if (prefix.isEmpty()) segment else "$prefix / $segment"
                val isLeaf = node !is SubmittedGroupNode
                entries.add(EstimationEntry(path, prefix, segment, depth, isLeaf, node.offerPT))
                if (!isLeaf) walk(node.children, path, depth + 1)
            }
        }
        walk(version.roots, "", 0)
        return entries
    }

    private fun computeDiff(
        activities: List<Activity>,
        entries: List<EstimationEntry>
    ): MerlinStructureDiff {
        val merlin = merlinPaths(activities)
        val merlinOrder = merlin.keys.toList()
        val estimationOrder = entries.map { it.path }
        val missingInMerlin = estimationOrder.filter { it !in merlin }
        val missingInEstimation = merlinOrder.filter { it !in estimationOrder.toSet() }
        val common = estimationOrder.filter { it in merlin }
        val commonMerlin = merlinOrder.filter { it in estimationOrder.toSet() }
        val reordered = common.filterIndexed { idx, path -> commonMerlin.getOrNull(idx) != path }
        return MerlinStructureDiff(missingInMerlin, missingInEstimation, reordered)
    }

    // ── Writing effort ──────────────────────────────────────────────────────

    private fun writeWork(
        conn: Connection,
        activities: List<Activity>,
        entries: List<EstimationEntry>
    ) {
        val merlin = merlinPaths(activities)
        var written = 0
        conn.prepareStatement(
            "UPDATE ZSCHEDULEITEM SET ZGIVENWORK_ = ?, Z_OPT = Z_OPT + 1 WHERE Z_PK = ?"
        ).use { st ->
            entries.filter { it.isLeaf }.forEach { entry ->
                val pk = merlin[entry.path] ?: return@forEach
                val blob = encodeWork(entry.offerPT)
                if (blob == null) st.setNull(1, java.sql.Types.BLOB) else st.setBytes(1, blob)
                st.setLong(2, pk)
                st.executeUpdate()
                written++
                Log.debug("Merlin export: wrote ${entry.offerPT} PT to activity $pk (${entry.path})")
            }
        }
        Log.info("Merlin export: wrote work to $written leaf activities")
    }

    // Merlin stores work as a short ASCII string plus a trailing 0x3F marker
    // byte (X'31643F' is "1d?"). The unit stays `d` — the one Merlin itself
    // wrote. An unestimated leaf gets NULL, as in an untouched document.
    fun encodeWork(personDays: Double): ByteArray? {
        if (personDays <= 0.0) return null
        return (formatDays(personDays) + "d").toByteArray(Charsets.US_ASCII) + 0x3F
    }

    fun formatDays(personDays: Double): String {
        val rounded = Math.round(personDays * 100.0) / 100.0
        return if (kotlin.math.abs(rounded - Math.round(rounded)) < 0.01) {
            Math.round(rounded).toString()
        } else {
            java.math.BigDecimal(rounded)
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
        }
    }

    // ── Structure overwrite ─────────────────────────────────────────────────

    // Reconciles by path rather than wiping and rebuilding, so activities that
    // still exist keep their row — and with it their Merlin-side dates,
    // dependencies and assignments.
    private fun reconcileStructure(
        conn: Connection,
        activities: List<Activity>,
        entries: List<EstimationEntry>
    ) {
        val root = activities.firstOrNull { it.parentPk == null }
            ?: throw IllegalArgumentException("Merlin document has no root activity")
        val merlin = merlinPaths(activities).toMutableMap()
        val wanted = entries.map { it.path }.toSet()

        deleteObsolete(conn, merlin, wanted)
        insertMissing(conn, entries, merlin, root.pk)
        repositionKept(conn, entries, merlin, root.pk)
        Log.info("Merlin export: structure overwritten to match the estimation (${entries.size} nodes)")
    }

    private fun deleteObsolete(conn: Connection, merlin: MutableMap<String, Long>, wanted: Set<String>) {
        val obsolete = merlin.filterKeys { it !in wanted }
        if (obsolete.isEmpty()) return
        val pks = obsolete.values.toList()
        val placeholders = pks.joinToString(",") { "?" }
        // Assignments reference their activity through ZACTIVITY_; drop them
        // first so no row is left dangling.
        conn.prepareStatement("DELETE FROM ZSCHEDULEITEM WHERE ZACTIVITY_ IN ($placeholders)").use { st ->
            pks.forEachIndexed { i, pk -> st.setLong(i + 1, pk) }
            st.executeUpdate()
        }
        conn.prepareStatement("DELETE FROM ZSCHEDULEITEM WHERE Z_PK IN ($placeholders)").use { st ->
            pks.forEachIndexed { i, pk -> st.setLong(i + 1, pk) }
            st.executeUpdate()
        }
        obsolete.keys.forEach { merlin.remove(it) }
        Log.debug("Merlin export: deleted ${pks.size} activities no longer in the estimation")
    }

    private fun insertMissing(
        conn: Connection,
        entries: List<EstimationEntry>,
        merlin: MutableMap<String, Long>,
        rootPk: Long
    ) {
        val missing = entries.filter { it.path !in merlin }
        if (missing.isEmpty()) return
        var nextPk = allocatePks(conn, missing.size)
        // Shallowest first, so a parent always exists before its children are
        // inserted and the parent lookup below resolves.
        missing.sortedBy { it.depth }.forEach { entry ->
            val parentPk = if (entry.parentPath.isEmpty()) rootPk else merlin[entry.parentPath] ?: rootPk
            val order = entries.indexOfFirst { it.path == entry.path }.toDouble()
            insertActivity(conn, nextPk, parentPk, order, entry.title)
            merlin[entry.path] = nextPk
            nextPk++
        }
        Log.debug("Merlin export: inserted ${missing.size} activities from the estimation")
    }

    // Core Data allocates primary keys from Z_PRIMARYKEY. ScheduleItem (Z_ENT
    // 48) is the ROOT of the entity hierarchy Activity (49) and Assignment (51)
    // inherit from, so its row holds the high-water mark; the subclass rows
    // carry Z_MAX = 0 and must not be used.
    private fun allocatePks(conn: Connection, count: Int): Long {
        var max = 0L
        conn.createStatement().use { st ->
            st.executeQuery("SELECT Z_MAX FROM Z_PRIMARYKEY WHERE Z_ENT = 48").use { rs ->
                if (rs.next()) max = rs.getLong(1)
            }
        }
        conn.prepareStatement("UPDATE Z_PRIMARYKEY SET Z_MAX = ? WHERE Z_ENT = 48").use { st ->
            st.setLong(1, max + count)
            st.executeUpdate()
        }
        return max + 1
    }

    private fun insertActivity(conn: Connection, pk: Long, parentPk: Long, order: Double, title: String) {
        conn.prepareStatement(
            "INSERT INTO ZSCHEDULEITEM (Z_PK, Z_ENT, Z_OPT, ZPROJECT, ZPRIORITY, ZBUDGETSTATUS_, " +
                "ZCOSTACCRUAL_, ZISBUDGETBILLABLE_, ZISMILESTONE, ZTITLE, ZPARENTACTIVITY_, " +
                "Z49_PARENTACTIVITY_, ZORDERINPARENTACTIVITY, ZUNIQUEID) " +
                "VALUES (?, 49, 1, 1, 500, 0, 2, 0, 0, ?, ?, 49, ?, ?)"
        ).use { st ->
            st.setLong(1, pk)
            st.setString(2, title)
            st.setLong(3, parentPk)
            st.setDouble(4, order)
            st.setString(5, newUniqueId())
            st.executeUpdate()
        }
    }

    private fun repositionKept(
        conn: Connection,
        entries: List<EstimationEntry>,
        merlin: Map<String, Long>,
        rootPk: Long
    ) {
        conn.prepareStatement(
            "UPDATE ZSCHEDULEITEM SET ZPARENTACTIVITY_ = ?, ZORDERINPARENTACTIVITY = ?, " +
                "Z_OPT = Z_OPT + 1 WHERE Z_PK = ?"
        ).use { st ->
            entries.forEachIndexed { idx, entry ->
                val pk = merlin[entry.path] ?: return@forEachIndexed
                val parentPk =
                    if (entry.parentPath.isEmpty()) rootPk else merlin[entry.parentPath] ?: rootPk
                st.setLong(1, parentPk)
                st.setDouble(2, idx.toDouble())
                st.setLong(3, pk)
                st.executeUpdate()
            }
        }
    }

    // Merlin's own ids are 16 random bytes in URL-safe base64 without padding.
    private fun newUniqueId(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
