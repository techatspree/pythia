package io.pythia.service

import io.pythia.domain.Estimation
import io.pythia.domain.EstimationBucket
import io.pythia.domain.draft.DraftBucketedItemNode
import io.pythia.domain.draft.DraftEstimationNode
import io.pythia.domain.draft.DraftEstimationVersion
import io.pythia.domain.draft.DraftFixedItemNode
import io.pythia.domain.draft.DraftGroupNode
import io.pythia.domain.draft.DraftScheduleDependency
import io.pythia.method.EstimationMethod
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import java.io.InputStream
import java.nio.file.Files
import java.sql.Connection
import java.util.Properties
import java.util.UUID
import java.util.zip.ZipInputStream

// Imports the Work Breakdown Structure of a Merlin Project document into a draft
// estimation version (task-131). A Merlin ".mproject" is a macOS bundle whose
// "state.sql" file is a SQLite 3 database (an Apple Core Data store, Z-prefixed
// tables); activities live in ZSCHEDULEITEM (title, parent, order, milestone,
// work). The WBS TREE (groups + nesting) is preserved for BOTH methods; the
// method only decides the leaf type: THREE_POINT_PERT gets fixed leaves,
// BUCKET_SAMPLED_PERT gets bucketed leaves assigned to one "Imported" bucket
// (buckets classify items, they do not replace the tree grouping).
//
// Dependencies come along too (task-221): ZDEPENDENCY rows (ZPREVIOUSACTIVITY_ →
// ZNEXTACTIVITY_, both ZSCHEDULEITEM.Z_PK) become the draft's finish-to-start
// schedule edges, between any two imported nodes, groups included (task-164).
// What Pythia cannot represent is skipped with a WARN, never fatal: another
// dependency type, an endpoint that is no imported node, a self-edge. A lag is
// dropped but the edge kept; duplicates collapse to one edge (the UNIQUE
// constraint would otherwise abort the whole import). No ZDEPENDENCY table means
// no edges.
//
// Column indices and unit factors here are the physical Merlin schema / the
// day-week-hour conversion (MagicNumber); the parser is inherently branchy
// (CyclomaticComplexMethod / ReturnCount) and the class is one cohesive importer
// split into small helpers (TooManyFunctions).
@Suppress("MagicNumber", "CyclomaticComplexMethod", "ReturnCount", "TooManyFunctions")
@ApplicationScoped
class MerlinImporter {

    private data class Activity(
        val pk: Long,
        val parentPk: Long?,
        val order: Double,
        val isMilestone: Boolean,
        val title: String,
        val workDays: Double
    )

    private data class MerlinDependency(
        val previousPk: Long?,
        val nextPk: Long?,
        val type: Int,
        val hasLag: Boolean
    )

    fun import(input: InputStream, estimation: Estimation, versionNumber: Int): DraftEstimationVersion {
        Log.info(
            "Importing Merlin project into estimation ${estimation.id} " +
                "version $versionNumber (method=${estimation.method})"
        )
        val sqliteBytes = extractSqlite(input.readBytes())
        val temp = Files.createTempFile("merlin-", ".sqlite")
        try {
            Files.write(temp, sqliteBytes)
            // Instantiate the driver directly (avoids DriverManager registration
            // quirks under Quarkus classloading); read-only, single ad-hoc file.
            org.sqlite.JDBC().connect("jdbc:sqlite:${temp.toAbsolutePath()}", Properties()).use { conn ->
                val activities = readActivities(conn)
                val dependencies = readDependencies(conn)
                return buildVersion(activities, dependencies, estimation, versionNumber)
            }
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    private fun extractSqlite(bytes: ByteArray): ByteArray = MerlinDocument.extractSqlite(bytes)

    private fun readActivities(conn: Connection): List<Activity> =
        conn.createStatement().use { st ->
            st.executeQuery(ACTIVITIES_QUERY).use { rs ->
                mapActivities(rs).also { Log.debug("Read ${it.size} Merlin activities") }
            }
        }

    // An older or stripped document may have no ZDEPENDENCY table: no edges, not an error.
    private fun readDependencies(conn: Connection): List<MerlinDependency> {
        val hasTable = conn.prepareStatement(DEPENDENCY_TABLE_QUERY).use { st ->
            st.executeQuery().use { it.next() }
        }
        if (!hasTable) {
            Log.debug("Merlin document has no ZDEPENDENCY table; importing no dependencies")
            return emptyList()
        }
        return conn.createStatement().use { st ->
            st.executeQuery(DEPENDENCIES_QUERY).use { rs ->
                mapDependencies(rs).also { Log.debug("Read ${it.size} Merlin dependencies") }
            }
        }
    }

    private fun mapDependencies(rs: java.sql.ResultSet): List<MerlinDependency> {
        val dependencies = mutableListOf<MerlinDependency>()
        while (rs.next()) {
            val previous = rs.getLong(1).let { if (rs.wasNull()) null else it }
            val next = rs.getLong(2).let { if (rs.wasNull()) null else it }
            dependencies.add(
                MerlinDependency(
                    previousPk = previous,
                    nextPk = next,
                    // A NULL ZTYPE reads as 0, Core Data's default for the attribute.
                    type = rs.getInt(3),
                    hasLag = rs.getInt(4) == 1
                )
            )
        }
        return dependencies
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
                    isMilestone = rs.getInt(4) == 1,
                    title = rs.getString(5).trim(),
                    workDays = parseWorkToPersonDays(rs.getString(6))
                )
            )
        }
        return activities
    }

    // Merlin stores work as a short string like "1d" (day), "2w" (week), "4h"
    // (hour), possibly with a trailing marker byte. Converts to person-days (PT)
    // assuming 8 h/day, 5 d/week; blank/unparseable → 0.0 (an unestimated leaf).
    fun parseWorkToPersonDays(raw: String?): Double {
        if (raw.isNullOrBlank()) return 0.0
        val cleaned = raw.trim().trimEnd { !it.isLetterOrDigit() }
        val match = Regex("^\\s*([0-9]+(?:[.,][0-9]+)?)\\s*([dwh]?)", RegexOption.IGNORE_CASE).find(cleaned)
            ?: return 0.0
        val value = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return 0.0
        val factor = when (match.groupValues[2].lowercase()) {
            "w" -> 5.0
            "h" -> 0.125
            else -> 1.0
        }
        return value * factor
    }

    private fun buildVersion(
        activities: List<Activity>,
        dependencies: List<MerlinDependency>,
        estimation: Estimation,
        versionNumber: Int
    ): DraftEstimationVersion {
        val version = DraftEstimationVersion().apply {
            this.estimation = estimation
            this.versionNumber = versionNumber
        }
        val byParent = activities.groupBy { it.parentPk }
        val root = activities.firstOrNull { it.parentPk == null }
            ?: throw IllegalArgumentException("Merlin document has no root activity")
        val topLevel = (byParent[root.pk] ?: emptyList()).sortedBy { it.order }

        // The WBS tree (groups + nesting) is preserved for BOTH methods; the
        // method only decides the LEAF type. For the bucket method every leaf is
        // assigned to a single "Imported" bucket (buckets classify items, they do
        // NOT replace the tree grouping).
        val bucket = if (estimation.method == EstimationMethod.BUCKET_SAMPLED_PERT) {
            ensureImportedBucket(estimation)
        } else {
            null
        }
        // Merlin Z_PK → the node built for it, groups included. The root
        // activity IS the estimation and gets no node, so it is not in here.
        val nodesByPk = mutableMapOf<Long, DraftEstimationNode>()
        topLevel.forEachIndexed { idx, act ->
            val node = buildNode(version, byParent, act, bucket, nodesByPk)
            node.parent = null
            node.position = idx
            version.roots.add(node)
        }
        val edges = applyDependencies(version, nodesByPk, dependencies)
        Log.info(
            "Imported Merlin WBS (${activities.size} activities) into estimation ${estimation.id} " +
                "as version $versionNumber (method=${estimation.method}), roots=${version.roots.size}, " +
                "dependencies=$edges"
        )
        return version
    }

    // Turns the Merlin dependencies into the draft's schedule edges and returns
    // how many were added. Rows are taken in Merlin row order; a duplicate pair
    // keeps its first occurrence.
    private fun applyDependencies(
        version: DraftEstimationVersion,
        nodesByPk: Map<Long, DraftEstimationNode>,
        dependencies: List<MerlinDependency>
    ): Int {
        val unsupportedTypes = sortedMapOf<Int, Int>()
        var unplaceable = 0
        var lagsIgnored = 0
        val edges = LinkedHashSet<Pair<String, String>>()
        for (dependency in dependencies) {
            val from = dependency.previousPk?.let { nodesByPk[it] }
            val to = dependency.nextPk?.let { nodesByPk[it] }
            when {
                // ZTYPE 0 = finish-to-start is taken from the OBSERVED data (both
                // rows of the demo file, Merlin's default type), not from a spec.
                // The other types (start-to-start, finish-to-finish,
                // start-to-finish) have no Pythia equivalent; importing them as
                // finish-to-start would invent a constraint nobody drew.
                dependency.type != FINISH_TO_START -> unsupportedTypes.merge(dependency.type, 1, Int::plus)
                from == null || to == null || from === to -> unplaceable++
                else -> {
                    // The lag's blob encoding is not decoded: the edge still
                    // orders the two activities, which is closer to the plan
                    // than no edge at all.
                    if (dependency.hasLag) lagsIgnored++
                    edges.add(from.logicalId.toString() to to.logicalId.toString())
                }
            }
        }
        edges.forEach { (fromId, toId) ->
            version.scheduleDependencies.add(DraftScheduleDependency().apply {
                this.fromLogicalId = fromId
                this.toLogicalId = toId
                this.version = version
            })
        }
        if (unsupportedTypes.isNotEmpty()) {
            Log.warn(
                "Skipped ${unsupportedTypes.values.sum()} Merlin dependencies of unsupported types " +
                    "(ZTYPE → count: $unsupportedTypes); only finish-to-start (0) is imported"
            )
        }
        if (unplaceable > 0) {
            Log.warn(
                "Skipped $unplaceable Merlin dependencies whose endpoint is not an imported activity " +
                    "or that point to themselves"
            )
        }
        if (lagsIgnored > 0) {
            Log.warn("Ignored the lag of $lagsIgnored imported Merlin dependencies (Pythia has no lags)")
        }
        return edges.size
    }

    // An activity with children is a group; a leaf carries the effort. A single
    // Merlin work value seeds optimistic = likely = pessimistic.
    private fun buildNode(
        version: DraftEstimationVersion,
        byParent: Map<Long?, List<Activity>>,
        activity: Activity,
        bucket: EstimationBucket?,
        nodesByPk: MutableMap<Long, DraftEstimationNode>
    ): DraftEstimationNode {
        val children = (byParent[activity.pk] ?: emptyList()).sortedBy { it.order }
        if (children.isEmpty()) {
            return buildLeaf(version, activity, bucket).also { nodesByPk[activity.pk] = it }
        }
        val group = DraftGroupNode().apply {
            this.version = version
            this.title = activity.title
        }
        nodesByPk[activity.pk] = group
        children.forEachIndexed { idx, child ->
            val childNode = buildNode(version, byParent, child, bucket, nodesByPk)
            childNode.parent = group
            childNode.position = idx
            group.children.add(childNode)
        }
        return group
    }

    // THREE_POINT_PERT → a fixed leaf; BUCKET_SAMPLED_PERT → a bucketed leaf
    // assigned to the imported bucket (a leaf with work is a sample).
    private fun buildLeaf(
        version: DraftEstimationVersion,
        activity: Activity,
        bucket: EstimationBucket?
    ): DraftEstimationNode {
        if (bucket == null) {
            return DraftFixedItemNode().apply {
                this.version = version
                this.description = activity.title
                this.minEffort = activity.workDays
                this.expectedEffort = activity.workDays
                this.maxEffort = activity.workDays
            }
        }
        val sample = activity.workDays > 0.0
        return DraftBucketedItemNode().apply {
            this.version = version
            this.description = activity.title
            this.bucket = bucket
            this.isSample = sample
            this.minEffort = if (sample) activity.workDays else null
            this.expectedEffort = if (sample) activity.workDays else null
            this.maxEffort = if (sample) activity.workDays else null
        }
    }

    private fun ensureImportedBucket(estimation: Estimation): EstimationBucket =
        estimation.buckets.firstOrNull() ?: EstimationBucket().apply {
            this.id = UUID.randomUUID()
            this.estimation = estimation
            this.position = 0
            this.label = "Imported"
        }.also { estimation.buckets.add(it) }

    internal companion object {
        // Activities carry a title; resource-assignment schedule items do not.
        // (Do NOT gate on Z_ENT — Core Data entity ids differ across exports.)
        internal const val ACTIVITIES_QUERY =
            "SELECT Z_PK, ZPARENTACTIVITY_, ZORDERINPARENTACTIVITY, ZISMILESTONE, ZTITLE, " +
                "CAST(ZGIVENWORK_ AS TEXT) FROM ZSCHEDULEITEM WHERE ZTITLE IS NOT NULL " +
                "ORDER BY ZORDERINPARENTACTIVITY"

        internal const val FINISH_TO_START = 0

        internal const val DEPENDENCY_TABLE_QUERY =
            "SELECT 1 FROM sqlite_master WHERE type='table' AND name='ZDEPENDENCY'"

        // Not gated on Z_ENT either, for the same reason as ACTIVITIES_QUERY.
        // ORDER BY Z_PK makes "Merlin row order" (the edges' order) explicit.
        internal const val DEPENDENCIES_QUERY = "SELECT ZPREVIOUSACTIVITY_, ZNEXTACTIVITY_, ZTYPE, " +
            "ZLAG_ IS NOT NULL AND length(ZLAG_) > 0 FROM ZDEPENDENCY ORDER BY Z_PK"
    }
}

// Container handling for a Merlin document, shared by the importer (task-131)
// and the exporter (task-133): a ".mproject" bundle is uploaded zipped (magic
// PK\x03\x04) and carries its SQLite store as a "state.sql" entry; otherwise
// the bytes must already be that SQLite document (magic "SQLite format 3"),
// i.e. the user uploaded state.sql directly.
internal object MerlinDocument {

    private const val ZIP_MAGIC_3 = 3.toByte()
    private const val ZIP_MAGIC_4 = 4.toByte()
    private val ZIP_MAGIC =
        byteArrayOf('P'.code.toByte(), 'K'.code.toByte(), ZIP_MAGIC_3, ZIP_MAGIC_4)
    private const val SQLITE_MAGIC = "SQLite format 3"

    fun isZip(bytes: ByteArray): Boolean =
        bytes.size >= ZIP_MAGIC.size && bytes.copyOfRange(0, ZIP_MAGIC.size).contentEquals(ZIP_MAGIC)

    fun isSqlite(bytes: ByteArray): Boolean =
        bytes.size >= SQLITE_MAGIC.length &&
            String(bytes, 0, SQLITE_MAGIC.length, Charsets.US_ASCII) == SQLITE_MAGIC

    fun extractSqlite(bytes: ByteArray): ByteArray {
        if (isZip(bytes)) return readStateSqlFromZip(bytes)
        require(isSqlite(bytes)) { "Uploaded file is neither a zipped .mproject nor a SQLite document" }
        return bytes
    }

    private fun readStateSqlFromZip(bytes: ByteArray): ByteArray {
        ZipInputStream(bytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.substringAfterLast('/') == "state.sql") {
                    return zip.readBytes()
                }
                entry = zip.nextEntry
            }
        }
        throw IllegalArgumentException("No state.sql found inside the uploaded Merlin .mproject archive")
    }

    // Rebuild an uploaded bundle with the modified store swapped in, leaving
    // every other entry of the .mproject byte-identical (export writes a COPY,
    // it does not repackage the user's project).
    fun repackWithSqlite(originalZip: ByteArray, sqlite: ByteArray): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(out).use { zos ->
            ZipInputStream(originalZip.inputStream()).use { zis -> copyEntries(zis, zos, sqlite) }
        }
        return out.toByteArray()
    }

    private fun copyEntries(
        zis: ZipInputStream,
        zos: java.util.zip.ZipOutputStream,
        sqlite: ByteArray
    ) {
        var entry = zis.nextEntry
        while (entry != null) {
            val isStore = !entry.isDirectory && entry.name.substringAfterLast('/') == "state.sql"
            val body = if (isStore) sqlite else zis.readBytes()
            zos.putNextEntry(java.util.zip.ZipEntry(entry.name))
            if (!entry.isDirectory) zos.write(body)
            zos.closeEntry()
            entry = zis.nextEntry
        }
    }
}
