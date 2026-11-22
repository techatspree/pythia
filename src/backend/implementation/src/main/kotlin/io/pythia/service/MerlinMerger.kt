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

// Merges an estimation into a Merlin Project document (task-133, renamed from
// "export" in task-223) — the mirror of MerlinImporter. Only ever edits a COPY:
// the uploaded bytes are written to a temp file, modified there, and returned;
// the user's own document is never touched. Everything Pythia does not own
// (dates, resources, assignments, calendars) stays as Merlin had it.
//
// What the merge writes, and what it reports back (MerlinMergeReport):
// - effort, only on leaves whose value CHANGES (compared at the precision the
//   writer stores, `formatDays` on both sides, so re-merging a merged file is a
//   no-op);
// - after confirmation, Pythia's structure (activities created, deleted with
//   their assignments and content — ObsoleteCascade —, moved);
// - Pythia's schedule dependencies, ADD-ONLY (DependencyMerge): a Merlin edge
//   Pythia lacks is never deleted, and an edge that would close a cycle or links
//   a group with its own sub-activity is skipped and reported.
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
// (CyclomaticComplexMethod) and this is one cohesive merger split into small
// helpers (TooManyFunctions).
@Suppress("MagicNumber", "CyclomaticComplexMethod", "TooManyFunctions")
@ApplicationScoped
class MerlinMerger(private val importer: MerlinImporter) {

    data class MerlinStructureDiff(
        val missingInMerlin: List<String>,
        val missingInEstimation: List<String>,
        val reordered: List<String>
    ) {
        val inSync: Boolean
            get() = missingInMerlin.isEmpty() && missingInEstimation.isEmpty() && reordered.isEmpty()
    }

    /**
     * What an overwrite's deletion removed (task-222): the obsolete activities
     * and both endpoints of every removed dependency as WBS paths, and the
     * number of rows removed per table. task-223's merge report lists these.
     */
    data class ObsoleteDeletion(
        val activities: List<String>,
        val removedDependencies: List<Pair<String, String>>,
        val removedRows: Map<String, Int>,
        // The deleted activities' assignment rows (task-223). removedRows counts
        // them inside ZSCHEDULEITEM together with the activities.
        val assignments: Int
    ) {
        companion object {
            val NONE = ObsoleteDeletion(emptyList(), emptyList(), emptyMap(), 0)
        }
    }

    private data class Activity(
        val pk: Long,
        val parentPk: Long?,
        val order: Double,
        val title: String,
        val work: String?
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
        val offerPT: Double,
        val logicalId: String
    )

    fun diff(input: ByteArray, version: SubmittedEstimationVersion): MerlinStructureDiff =
        withDocument(input) { conn -> computeDiff(readActivities(conn), estimationEntries(version)) }

    fun merge(
        input: ByteArray,
        version: SubmittedEstimationVersion,
        overwriteStructure: Boolean
    ): MerlinMergeResult {
        val entries = estimationEntries(version)
        Log.info(
            "Merging estimation ${version.estimation?.id} version ${version.versionNumber} " +
                "into a Merlin copy (${entries.count { it.isLeaf }} leaves, " +
                "overwriteStructure=$overwriteStructure)"
        )
        val (modified, report) = modifyDocument(input, version, entries, overwriteStructure)
        val result = if (MerlinDocument.isZip(input)) {
            MerlinDocument.repackWithSqlite(input, modified)
        } else {
            modified
        }
        Log.info(
            "Merlin merge finished for estimation ${version.estimation?.id} (${result.size} bytes, " +
                "${report.effortChanges.size} effort changes, ${report.dependenciesAdded.size} dependencies added)"
        )
        return MerlinMergeResult(result, report)
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
    ): Pair<ByteArray, MerlinMergeReport> {
        val sqlite = MerlinDocument.extractSqlite(input)
        val temp = Files.createTempFile("merlin-merge-", ".sqlite")
        try {
            Files.write(temp, sqlite)
            val report = openConnection(temp).use { conn ->
                applyToConnection(conn, version, entries, overwriteStructure)
            }
            return Files.readAllBytes(temp) to report
        } catch (e: MerlinStructureChangedException) {
            throw e
        } catch (e: Exception) {
            Log.error("Merlin merge failed for estimation ${version.estimation?.id}", e)
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
    ): MerlinMergeReport {
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
        val deletion = if (structureDiff.inSync) {
            ObsoleteDeletion.NONE
        } else {
            reconcileStructure(conn, activities, entries)
        }
        val current = if (structureDiff.inSync) activities else readActivities(conn)
        val effortChanges = writeWork(conn, current, entries)
        val dependencies = mergeDependencies(conn, version, entries, current)
        conn.commit()
        val applied = !structureDiff.inSync
        return MerlinMergeReport(
            effortChanges = effortChanges,
            activitiesAdded = if (applied) structureDiff.missingInMerlin else emptyList(),
            activitiesRemoved = deletion.activities,
            activitiesMoved = if (applied) structureDiff.reordered else emptyList(),
            dependenciesAdded = dependencies.added,
            dependenciesRemoved = deletion.removedDependencies.map { (from, to) -> MerlinMergedDependency(from, to) },
            dependenciesSkipped = dependencies.skipped,
            contentRemoved = MerlinRemovedContent.of(deletion)
        )
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
                    title = rs.getString(5).trim(),
                    work = rs.getString(6)
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
                entries.add(
                    EstimationEntry(path, prefix, segment, depth, isLeaf, node.offerPT, node.logicalId.toString())
                )
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

    // Writes only where the value CHANGES. Both sides are compared as
    // `formatDays` strings — the 2-decimal form the writer stores — so a value
    // this merge wrote earlier compares equal exactly; a numeric tolerance could
    // land on formatDays' HALF_UP boundary. "No work" (NULL / offerPT <= 0) is
    // null on both sides.
    private fun writeWork(
        conn: Connection,
        activities: List<Activity>,
        entries: List<EstimationEntry>
    ): List<MerlinEffortChange> {
        val merlin = merlinPaths(activities)
        val workByPk = activities.associate { it.pk to it.work }
        val changes = mutableListOf<MerlinEffortChange>()
        conn.prepareStatement(
            "UPDATE ZSCHEDULEITEM SET ZGIVENWORK_ = ?, Z_OPT = Z_OPT + 1 WHERE Z_PK = ?"
        ).use { st ->
            entries.filter { it.isLeaf }.forEach { entry ->
                val pk = merlin[entry.path] ?: return@forEach
                val before = importer.parseWorkToPersonDays(workByPk[pk]).takeIf { it > 0.0 }
                val after = entry.offerPT.takeIf { it > 0.0 }
                if (before?.let(::formatDays) == after?.let(::formatDays)) return@forEach
                val blob = encodeWork(entry.offerPT)
                if (blob == null) st.setNull(1, java.sql.Types.BLOB) else st.setBytes(1, blob)
                st.setLong(2, pk)
                st.executeUpdate()
                changes.add(MerlinEffortChange(entry.path, before, after?.let { Math.round(it * 100.0) / 100.0 }))
                Log.debug("Merlin merge: wrote ${entry.offerPT} PT to activity $pk (${entry.path}), was $before")
            }
        }
        Log.info("Merlin merge: wrote work to ${changes.size} leaf activities whose value changed")
        return changes
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
    // dependencies and assignments. An obsolete activity is deleted together
    // with everything that refers to it (ObsoleteCascade, task-222), so no row
    // of the returned document points at a row that is gone.
    private fun reconcileStructure(
        conn: Connection,
        activities: List<Activity>,
        entries: List<EstimationEntry>
    ): ObsoleteDeletion {
        val root = activities.firstOrNull { it.parentPk == null }
            ?: throw IllegalArgumentException("Merlin document has no root activity")
        val merlin = merlinPaths(activities).toMutableMap()
        val wanted = entries.map { it.path }.toSet()

        val deletion = deleteObsolete(conn, merlin, wanted)
        if (deletion.activities.isNotEmpty()) {
            Log.info(
                "Merlin merge: deleted ${deletion.activities.size} activities no longer in the estimation; " +
                    "rows removed per table: ${deletion.removedRows}"
            )
            deletion.removedDependencies.forEach { (from, to) ->
                Log.debug("Merlin merge: removed dependency $from → $to")
            }
        }
        insertMissing(conn, entries, merlin, root.pk)
        repositionKept(conn, entries, merlin, root.pk)
        Log.info("Merlin merge: structure overwritten to match the estimation (${entries.size} nodes)")
        return deletion
    }

    private fun deleteObsolete(
        conn: Connection,
        merlin: MutableMap<String, Long>,
        wanted: Set<String>
    ): ObsoleteDeletion {
        val obsolete = merlin.filterKeys { it !in wanted }
        if (obsolete.isEmpty()) return ObsoleteDeletion.NONE
        // The FULL pre-deletion path map: a removed dependency's other end may
        // be a kept activity.
        val pathByPk = merlin.entries.associate { (path, pk) -> pk to path }
        val deletion = ObsoleteCascade(conn).delete(obsolete.keys.toList(), obsolete.values.toSet(), pathByPk)
        obsolete.keys.forEach { merlin.remove(it) }
        return deletion
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
        Log.debug("Merlin merge: inserted ${missing.size} activities from the estimation")
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

    // ── Dependencies ────────────────────────────────────────────────────────

    // Pythia's schedule edges → Merlin activity pks, then DependencyMerge adds
    // the ones Merlin lacks. Runs on the activities as they are AFTER any
    // structure overwrite, so an edge of a re-created activity resolves to its
    // new row. An edge whose node is not in the version, or whose path has no
    // activity, is skipped silently — the domain ignores such edges too.
    private fun mergeDependencies(
        conn: Connection,
        version: SubmittedEstimationVersion,
        entries: List<EstimationEntry>,
        activities: List<Activity>
    ): DependencyMerge.Outcome {
        val pathByLogicalId = entries.associate { it.logicalId to it.path }
        val merlin = merlinPaths(activities)
        val edges = version.scheduleDependencies.mapNotNull { dependency ->
            val from = pathByLogicalId[dependency.fromLogicalId]?.takeIf { it in merlin }
            val to = pathByLogicalId[dependency.toLogicalId]?.takeIf { it in merlin }
            if (from == null || to == null) {
                Log.debug(
                    "Merlin merge: edge ${dependency.fromLogicalId} -> ${dependency.toLogicalId} " +
                        "has no Merlin activity on both ends; skipped"
                )
                null
            } else {
                DependencyMerge.Edge(from, to, merlin.getValue(from), merlin.getValue(to))
            }
        }
        val root = activities.firstOrNull { it.parentPk == null }
        val outcome = DependencyMerge(conn, activities.associate { it.pk to it.parentPk }, root?.pk).merge(edges)
        Log.info(
            "Merlin merge: ${outcome.added.size} dependencies added, ${outcome.skipped.size} skipped " +
                "(${version.scheduleDependencies.size} Pythia edges, ${edges.size} mapped onto Merlin activities)"
        )
        return outcome
    }
}

// Merlin's own ids are 16 random bytes in URL-safe base64 without padding.
private const val UNIQUE_ID_BYTES = 16

private fun newUniqueId(): String {
    val bytes = ByteArray(UNIQUE_ID_BYTES)
    SecureRandom().nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

// ── The merge's result (task-223) ───────────────────────────────────────────

// A plain class, not a data class: equality over a ByteArray would be identity.
class MerlinMergeResult(val document: ByteArray, val report: MerlinMergeReport)

data class MerlinMergeReport(
    val effortChanges: List<MerlinEffortChange>,
    val activitiesAdded: List<String>,
    val activitiesRemoved: List<String>,
    val activitiesMoved: List<String>,
    val dependenciesAdded: List<MerlinMergedDependency>,
    val dependenciesRemoved: List<MerlinMergedDependency>,
    val dependenciesSkipped: List<MerlinSkippedDependency>,
    val contentRemoved: MerlinRemovedContent
)

// null = no work on that side.
data class MerlinEffortChange(val path: String, val beforePT: Double?, val afterPT: Double?)

data class MerlinMergedDependency(val fromPath: String, val toPath: String)

data class MerlinSkippedDependency(
    val fromPath: String,
    val toPath: String,
    val reason: MerlinDependencySkipReason
)

enum class MerlinDependencySkipReason {
    // Merlin already leads from the edge's successor back to its predecessor.
    WOULD_CREATE_CYCLE,

    // One end is an ancestor of the other: a group's dates come from its own
    // children, so such a link is circular by construction.
    PARENT_CHILD
}

// User content deleted together with obsolete activities. Comment readings,
// view configurations and list-value joins are bookkeeping, not reported.
data class MerlinRemovedContent(
    val assignments: Int,
    val comments: Int,
    val attachments: Int,
    val costs: Int,
    val customAttributeValues: Int
) {
    companion object {
        fun of(deletion: MerlinMerger.ObsoleteDeletion) = MerlinRemovedContent(
            assignments = deletion.assignments,
            comments = deletion.removedRows["ZCOMMENT"] ?: 0,
            attachments = deletion.removedRows["ZATTACHMENT"] ?: 0,
            costs = deletion.removedRows["ZCOST"] ?: 0,
            customAttributeValues = deletion.removedRows["ZCUSTOMATTRIBUTEVALUE"] ?: 0
        )
    }
}

// Adds Pythia's finish-to-start edges to a Merlin document, ADD-ONLY (task-223):
// a Merlin dependency Pythia lacks is never deleted, because the merge cannot
// tell "deleted in Pythia" from "added in Merlin". Per edge, in order:
// - already present from → to (any ZTYPE): nothing to do;
// - one end an ancestor of the other: skipped, PARENT_CHILD;
// - `to` already reaches `from` through the document's edges (including the
//   ones added so far): skipped, WOULD_CREATE_CYCLE;
// - otherwise INSERTed with ZTYPE 0 and no lag. Z_ENT and the pk come from the
//   Z_PRIMARYKEY row named 'Dependency', read by name like any Core Data id.
// MagicNumber: JDBC parameter indices of the INSERT.
@Suppress("MagicNumber")
private class DependencyMerge(
    private val conn: Connection,
    private val parentByPk: Map<Long, Long?>,
    private val rootPk: Long?
) {

    data class Edge(val fromPath: String, val toPath: String, val fromPk: Long, val toPk: Long)

    data class Outcome(val added: List<MerlinMergedDependency>, val skipped: List<MerlinSkippedDependency>)

    fun merge(edges: List<Edge>): Outcome {
        val entity = if (edges.isEmpty()) null else dependencyEntity()
        if (entity == null) {
            if (edges.isNotEmpty()) {
                Log.warn("Merlin merge: document cannot hold dependencies; ${edges.size} edges not added")
            }
            return Outcome(emptyList(), emptyList())
        }
        val successors = readSuccessors()
        val added = mutableListOf<Edge>()
        val skipped = mutableListOf<MerlinSkippedDependency>()
        edges.forEach { edge ->
            val reason = when {
                successors[edge.fromPk]?.contains(edge.toPk) == true -> null
                isAncestor(edge.fromPk, edge.toPk) || isAncestor(edge.toPk, edge.fromPk) ->
                    MerlinDependencySkipReason.PARENT_CHILD
                reaches(edge.toPk, edge.fromPk, successors) -> MerlinDependencySkipReason.WOULD_CREATE_CYCLE
                else -> {
                    successors.getOrPut(edge.fromPk) { mutableSetOf() }.add(edge.toPk)
                    added.add(edge)
                    null
                }
            }
            if (reason != null) skipped.add(MerlinSkippedDependency(edge.fromPath, edge.toPath, reason))
            Log.debug("Merlin merge: edge ${edge.fromPath} → ${edge.toPath}: ${reason ?: "added or present"}")
        }
        insert(added, entity)
        return Outcome(added.map { MerlinMergedDependency(it.fromPath, it.toPath) }, skipped)
    }

    // (Z_ENT, Z_MAX) of the Dependency entity, or null when the document has
    // no ZDEPENDENCY table or no such Z_PRIMARYKEY row.
    private fun dependencyEntity(): Pair<Int, Long>? {
        val hasTable = conn.prepareStatement(MerlinImporter.DEPENDENCY_TABLE_QUERY).use { st ->
            st.executeQuery().use { it.next() }
        }
        return if (hasTable) readEntityRow() else null
    }

    private fun readEntityRow(): Pair<Int, Long>? =
        conn.prepareStatement("SELECT Z_ENT, Z_MAX FROM Z_PRIMARYKEY WHERE Z_NAME = 'Dependency'").use { st ->
            st.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) to rs.getLong(2) else null }
        }

    private fun projectOf(pk: Long): Long? =
        conn.prepareStatement("SELECT ZPROJECT FROM ZSCHEDULEITEM WHERE Z_PK = ?").use { st ->
            st.setLong(1, pk)
            st.executeQuery().use { rs -> if (rs.next()) rs.getLong(1) else null }
        }

    private fun readSuccessors(): MutableMap<Long, MutableSet<Long>> {
        val successors = mutableMapOf<Long, MutableSet<Long>>()
        conn.prepareStatement("SELECT ZPREVIOUSACTIVITY_, ZNEXTACTIVITY_ FROM ZDEPENDENCY").use { st ->
            st.executeQuery().use { rs ->
                while (rs.next()) successors.getOrPut(rs.getLong(1)) { mutableSetOf() }.add(rs.getLong(2))
            }
        }
        return successors
    }

    private fun isAncestor(ancestor: Long, of: Long): Boolean =
        generateSequence(parentByPk[of]) { parentByPk[it] }.any { it == ancestor }

    // Depth-first: can `from` reach `target` along the edges?
    private fun reaches(from: Long, target: Long, successors: Map<Long, Set<Long>>): Boolean {
        val seen = mutableSetOf<Long>()
        val stack = ArrayDeque(listOf(from))
        while (stack.isNotEmpty()) {
            val pk = stack.removeLast()
            if (pk == target) return true
            if (seen.add(pk)) stack.addAll(successors[pk].orEmpty())
        }
        return false
    }

    private fun insert(edges: List<Edge>, entity: Pair<Int, Long>) {
        if (edges.isEmpty()) return
        val (ent, max) = entity
        val project = rootPk?.let(::projectOf)
        conn.prepareStatement(
            "INSERT INTO ZDEPENDENCY (Z_PK, Z_ENT, Z_OPT, ZTYPE, ZPREVIOUSACTIVITY_, Z49_PREVIOUSACTIVITY_, " +
                "ZNEXTACTIVITY_, Z49_NEXTACTIVITY_, ZPROJECT, ZUNIQUEID, ZLAG_) " +
                "VALUES (?, ?, 1, ${MerlinImporter.FINISH_TO_START}, ?, $ACTIVITY_ENTITY, " +
                "?, $ACTIVITY_ENTITY, ?, ?, NULL)"
        ).use { st ->
            edges.forEachIndexed { i, edge ->
                st.setLong(1, max + i + 1)
                st.setInt(2, ent)
                st.setLong(3, edge.fromPk)
                st.setLong(4, edge.toPk)
                if (project == null) st.setNull(5, java.sql.Types.INTEGER) else st.setLong(5, project)
                st.setString(6, newUniqueId())
                st.executeUpdate()
            }
        }
        conn.prepareStatement("UPDATE Z_PRIMARYKEY SET Z_MAX = ? WHERE Z_NAME = 'Dependency'").use { st ->
            st.setLong(1, max + edges.size)
            st.executeUpdate()
        }
    }

    private companion object {
        // The Activity entity's id, the assumption insertActivity already makes.
        const val ACTIVITY_ENTITY = 49
    }
}

// Deletes obsolete activities together with every row that refers to them
// (task-222), so an overwritten document holds no reference to a row that is
// gone. What goes is what Merlin itself removes with a deleted activity:
//
// - the activities' ASSIGNMENTS (ZSCHEDULEITEM.ZACTIVITY_); from here on the
//   "deleted schedule items" are the activities AND their assignments, since
//   anything below may name either;
// - content OWNED by a deleted item, recursively: comments (and their
//   readings) and attachments (and everything that hangs off them). Ownership
//   is circular — a comment may belong to an attachment and an attachment to a
//   comment (ZATTACHMENT.ZCOMMENT) — so both sets are collected until nothing
//   new is found;
// - references with no content of their own: dependencies, view
//   configurations, list-value join rows, costs, custom attribute values.
//
// The list is EXPLICIT (REFERENCES, measured on the demo file by scanning every
// column), not discovered at runtime: a scan would also delete through columns
// whose meaning nobody checked. A table or column a document lacks (an older
// Merlin) is skipped. NOT references despite their names:
// ZGIVENPERMISSIONFORACTIVITIES / ZGIVENPERMISSIONFORATTACHMENTS on ZRESOURCE
// and ZRESOURCEGROUP (integer flags), and ZPROJECT.ZROOTACTIVITY (the root is
// never obsolete). The Z1_/Z48_/Z49_ columns beside a reference are Core
// Data's entity-type columns for it and go with their row.
//
// Rank-chain links of KEPT activities that pointed at a deleted one are cleared
// (with their Z49_ entity-type column), not cascaded.
//
// Whether Merlin accepts the result is verified manually only (it is a macOS
// app, not available in CI).
private class ObsoleteCascade(private val conn: Connection) {

    private enum class Target { SCHEDULE_ITEM, COMMENT, ATTACHMENT }

    private data class Reference(val table: String, val column: String, val target: Target)

    private val columnPresence = mutableMapOf<Pair<String, String>, Boolean>()

    fun delete(
        obsoletePaths: List<String>,
        obsoletePks: Set<Long>,
        pathByPk: Map<Long, String>
    ): MerlinMerger.ObsoleteDeletion {
        val assignments = select("ZSCHEDULEITEM", "ZACTIVITY_", obsoletePks)
        val items = obsoletePks + assignments
        val comments = mutableSetOf<Long>()
        val attachments = mutableSetOf<Long>()
        do {
            val before = comments.size + attachments.size
            attachments += select("ZATTACHMENT", "ZSCHEDULEITEM", items)
            attachments += select("ZATTACHMENT", "ZCOMMENT", comments)
            comments += select("ZCOMMENT", "ZSCHEDULEITEM", items)
            comments += select("ZCOMMENT", "ZATTACHMENT", attachments)
        } while (comments.size + attachments.size != before)

        val removedDependencies = readDependencies(items).map { (from, to) ->
            label(from, pathByPk) to label(to, pathByPk)
        }
        val targets = mapOf(
            Target.SCHEDULE_ITEM to items,
            Target.COMMENT to comments,
            Target.ATTACHMENT to attachments
        )
        val removedRows = sortedMapOf<String, Int>()
        REFERENCES.forEach { ref ->
            val removed = deleteWhere(ref.table, ref.column, targets.getValue(ref.target))
            if (removed > 0) removedRows.merge(ref.table, removed, Int::plus)
        }
        clearRankChain(items)
        return MerlinMerger.ObsoleteDeletion(obsoletePaths, removedDependencies, removedRows, assignments.size)
    }

    private fun label(pk: Long, pathByPk: Map<Long, String>): String = pathByPk[pk] ?: "#$pk"

    private fun hasColumn(table: String, column: String): Boolean =
        columnPresence.getOrPut(table to column) {
            conn.prepareStatement("SELECT 1 FROM pragma_table_info(?) WHERE name = ?").use { st ->
                st.setString(1, table)
                st.setString(2, column)
                st.executeQuery().use { it.next() }
            }.also { present ->
                if (!present) Log.debug("Merlin merge: document has no $table.$column; skipped")
            }
        }

    // Table and column names come from the constant list above, never from
    // input; the ids are bound.
    private fun select(table: String, column: String, ids: Set<Long>): Set<Long> {
        if (ids.isEmpty() || !hasColumn(table, column)) return emptySet()
        val sql = "SELECT Z_PK FROM $table WHERE $column IN (${placeholders(ids)})"
        return conn.prepareStatement(sql).use { st ->
            bind(st, ids)
            st.executeQuery().use { rs ->
                buildSet { while (rs.next()) add(rs.getLong(1)) }
            }
        }
    }

    private fun readDependencies(items: Set<Long>): List<Pair<Long, Long>> {
        val columns = listOf("ZPREVIOUSACTIVITY_", "ZNEXTACTIVITY_")
        if (items.isEmpty() || columns.any { !hasColumn("ZDEPENDENCY", it) }) return emptyList()
        val sql = "SELECT ZPREVIOUSACTIVITY_, ZNEXTACTIVITY_ FROM ZDEPENDENCY " +
            "WHERE ZPREVIOUSACTIVITY_ IN (${placeholders(items)}) OR ZNEXTACTIVITY_ IN (${placeholders(items)}) " +
            "ORDER BY Z_PK"
        return conn.prepareStatement(sql).use { st ->
            bind(st, items)
            bind(st, items, offset = items.size)
            st.executeQuery().use { rs ->
                buildList { while (rs.next()) add(rs.getLong(1) to rs.getLong(2)) }
            }
        }
    }

    private fun deleteWhere(table: String, column: String, ids: Set<Long>): Int {
        if (ids.isEmpty() || !hasColumn(table, column)) return 0
        return conn.prepareStatement("DELETE FROM $table WHERE $column IN (${placeholders(ids)})").use { st ->
            bind(st, ids)
            st.executeUpdate()
        }
    }

    private fun clearRankChain(deleted: Set<Long>) {
        RANK_COLUMNS.filter { hasColumn("ZSCHEDULEITEM", it) }.forEach { column ->
            val entityColumn = "Z49_$column"
            val clearEntity = if (hasColumn("ZSCHEDULEITEM", entityColumn)) ", $entityColumn = NULL" else ""
            val sql = "UPDATE ZSCHEDULEITEM SET $column = NULL$clearEntity, Z_OPT = Z_OPT + 1 " +
                "WHERE $column IN (${placeholders(deleted)})"
            val cleared = conn.prepareStatement(sql).use { st ->
                bind(st, deleted)
                st.executeUpdate()
            }
            if (cleared > 0) Log.debug("Merlin merge: cleared $cleared $column links to deleted activities")
        }
    }

    private fun placeholders(ids: Set<Long>): String = ids.joinToString(",") { "?" }

    private fun bind(st: java.sql.PreparedStatement, ids: Set<Long>, offset: Int = 0) {
        ids.forEachIndexed { i, id -> st.setLong(offset + i + 1, id) }
    }

    private companion object {
        val RANK_COLUMNS = listOf("ZRANKNEXTACTIVITY_", "ZRANKPREVIOUSACTIVITY_")

        // Deleted in this order: dependents first, then the owners, then the
        // schedule items themselves (activities and their assignments).
        val REFERENCES = listOf(
            Reference("ZCOMMENTREADING", "ZCOMMENT", Target.COMMENT),
            Reference("Z_48ASSOCIATEDLISTVALUES", "Z_48SCHEDULEITEMS", Target.SCHEDULE_ITEM),
            Reference("Z_1ASSOCIATEDLISTVALUES", "Z_1ATTACHMENTS", Target.ATTACHMENT),
            Reference("ZOBJECTVIEWCONFIGURATION", "ZSCHEDULEITEM_", Target.SCHEDULE_ITEM),
            Reference("ZOBJECTVIEWCONFIGURATION", "ZATTACHMENT_", Target.ATTACHMENT),
            Reference("ZCOST", "ZSCHEDULEITEM", Target.SCHEDULE_ITEM),
            Reference("ZCOST", "ZATTACHMENT", Target.ATTACHMENT),
            Reference("ZCUSTOMATTRIBUTEVALUE", "ZSCHEDULEITEM", Target.SCHEDULE_ITEM),
            Reference("ZCUSTOMATTRIBUTEVALUE", "ZATTACHMENT", Target.ATTACHMENT),
            Reference("ZDEPENDENCY", "ZPREVIOUSACTIVITY_", Target.SCHEDULE_ITEM),
            Reference("ZDEPENDENCY", "ZNEXTACTIVITY_", Target.SCHEDULE_ITEM),
            Reference("ZCOMMENT", "Z_PK", Target.COMMENT),
            Reference("ZATTACHMENT", "Z_PK", Target.ATTACHMENT),
            Reference("ZSCHEDULEITEM", "Z_PK", Target.SCHEDULE_ITEM)
        )
    }
}
