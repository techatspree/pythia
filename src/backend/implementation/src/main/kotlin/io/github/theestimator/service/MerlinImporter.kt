package io.github.theestimator.service

import io.github.theestimator.domain.Estimation
import io.github.theestimator.domain.EstimationBucket
import io.github.theestimator.domain.draft.DraftBucketedItemNode
import io.github.theestimator.domain.draft.DraftEstimationNode
import io.github.theestimator.domain.draft.DraftEstimationVersion
import io.github.theestimator.domain.draft.DraftFixedItemNode
import io.github.theestimator.domain.draft.DraftGroupNode
import io.github.theestimator.method.EstimationMethod
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
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

    fun import(input: InputStream, estimation: Estimation, versionNumber: Int): DraftEstimationVersion {
        Log.info(
            "Importing Merlin project into estimation ${estimation.id} " +
                "version $versionNumber (method=${estimation.method})"
        )
        val sqliteBytes = extractSqlite(input.readBytes())
        val temp = Files.createTempFile("merlin-", ".sqlite")
        try {
            Files.write(temp, sqliteBytes)
            val activities = readActivities(temp)
            return buildVersion(activities, estimation, versionNumber)
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    private fun extractSqlite(bytes: ByteArray): ByteArray = MerlinDocument.extractSqlite(bytes)

    private fun readActivities(dbFile: Path): List<Activity> {
        // Instantiate the driver directly (avoids DriverManager registration
        // quirks under Quarkus classloading); read-only, single ad-hoc file.
        org.sqlite.JDBC().connect("jdbc:sqlite:${dbFile.toAbsolutePath()}", Properties()).use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery(ACTIVITIES_QUERY).use { rs ->
                    val activities = mapActivities(rs)
                    Log.debug("Read ${activities.size} Merlin activities")
                    return activities
                }
            }
        }
        return emptyList()
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
        topLevel.forEachIndexed { idx, act ->
            val node = buildNode(version, byParent, act, bucket)
            node.parent = null
            node.position = idx
            version.roots.add(node)
        }
        Log.info(
            "Imported Merlin WBS (${activities.size} activities) into estimation ${estimation.id} " +
                "as version $versionNumber (method=${estimation.method}), roots=${version.roots.size}"
        )
        return version
    }

    // An activity with children is a group; a leaf carries the effort. A single
    // Merlin work value seeds optimistic = likely = pessimistic.
    private fun buildNode(
        version: DraftEstimationVersion,
        byParent: Map<Long?, List<Activity>>,
        activity: Activity,
        bucket: EstimationBucket?
    ): DraftEstimationNode {
        val children = (byParent[activity.pk] ?: emptyList()).sortedBy { it.order }
        if (children.isEmpty()) {
            return buildLeaf(version, activity, bucket)
        }
        val group = DraftGroupNode().apply {
            this.version = version
            this.title = activity.title
        }
        children.forEachIndexed { idx, child ->
            val childNode = buildNode(version, byParent, child, bucket)
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
