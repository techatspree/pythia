package io.pythia.service

import io.pythia.domain.Estimation
import io.pythia.domain.draft.DraftAdditionalCost
import io.pythia.domain.draft.DraftEffortDriver
import io.pythia.domain.draft.DraftEstimationNode
import io.pythia.domain.draft.DraftEstimationVersion
import io.pythia.domain.draft.DraftBucketedItemNode
import io.pythia.domain.draft.DraftFixedItemNode
import io.pythia.domain.draft.DraftGroupNode
import io.pythia.domain.draft.DraftProjectPhase
import io.pythia.domain.draft.DraftScheduleDependency
import io.pythia.domain.draft.DraftTimeRelativeItemNode
import io.pythia.domain.submitted.SubmittedAdditionalCost
import io.pythia.domain.submitted.SubmittedEffortDriver
import io.pythia.domain.submitted.SubmittedEstimationNode
import io.pythia.domain.submitted.SubmittedEstimationVersion
import io.pythia.domain.submitted.SubmittedBucketedItemNode
import io.pythia.domain.submitted.SubmittedFixedItemNode
import io.pythia.domain.submitted.SubmittedGroupNode
import io.pythia.domain.submitted.SubmittedProjectPhase
import io.pythia.domain.submitted.SubmittedScheduleDependency
import io.pythia.domain.submitted.SubmittedTimeRelativeItemNode
import io.pythia.method.EstimationMethod
import io.pythia.method.bucketsampled.BucketedEstimationItem
import io.pythia.method.threepoint.FixedEstimationItem
import io.pythia.method.threepoint.TimeRelativeEstimationItem
import io.pythia.model.EstimationGroup
import io.pythia.model.EstimationItem
import io.pythia.model.EstimationNode
import io.pythia.model.EstimationVersion
import io.pythia.repository.DraftEstimationVersionRepository
import io.pythia.repository.EstimationRepository
import io.pythia.repository.SubmittedEstimationVersionRepository
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import org.apache.poi.ooxml.POIXMLException
import org.apache.poi.openxml4j.exceptions.OpenXML4JRuntimeException
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import kotlin.io.path.inputStream

// The snapshot/clone tree builders are split into focused private helpers to
// keep each method's complexity low; that deliberately raises the class's
// function count past the TooManyFunctions threshold for one cohesive service.
// LongParameterList is likewise deliberate: these are DI-injected collaborators
// on a single cohesive service, not a call-site argument list.

// 422 Unprocessable Content: jakarta's `Response.Status` has no constant for it,
// so the code is named here rather than left as a bare literal. It marks an xlsx
// whose method disagrees with the estimation's (task-183) — a perfectly valid
// workbook that simply cannot be applied HERE, which the UI tells apart from an
// unreadable upload (400) by status alone.
private const val UNPROCESSABLE_CONTENT = 422

@Suppress("TooManyFunctions", "LongParameterList")
@ApplicationScoped
class EstimationVersionService(
    private val draftRepository: DraftEstimationVersionRepository,
    private val submittedRepository: SubmittedEstimationVersionRepository,
    private val estimationRepository: EstimationRepository,
    private val draftVersionMapper: DraftVersionMapper,
    private val auditLogService: AuditLogService,
    private val merlinImporter: MerlinImporter,
    private val excelImporter: ExcelImporter,
    private val systemSettingsService: SystemSettingsService
) {

    fun findDraft(estimationId: UUID): DraftEstimationVersion? =
        draftRepository.findByEstimationId(estimationId)

    fun findSubmittedVersions(estimationId: UUID): List<SubmittedEstimationVersion> =
        submittedRepository.findByEstimationId(estimationId)

    fun findSubmittedVersion(estimationId: UUID, versionNumber: Int): SubmittedEstimationVersion? =
        submittedRepository.findByEstimationIdAndVersionNumber(estimationId, versionNumber)

    fun calculateDraft(draft: DraftEstimationVersion): EstimationVersion =
        draftVersionMapper.toDomain(draft).calculate()

    @Transactional
    fun createDraft(estimationId: UUID): DraftEstimationVersion {
        Log.info("Creating draft for estimation $estimationId")
        val estimation = estimationRepository.findById(estimationId)
            ?: run {
                Log.error("Cannot create draft: estimation $estimationId not found")
                throw WebApplicationException("Estimation not found: $estimationId", Response.Status.NOT_FOUND)
            }

        if (draftRepository.findByEstimationId(estimationId) != null) {
            throw WebApplicationException(
                "A draft already exists for estimation $estimationId",
                Response.Status.CONFLICT
            )
        }

        val latestSubmitted = submittedRepository.findLatestByEstimationId(estimationId)
        val newVersionNumber = (latestSubmitted?.versionNumber ?: 0) + 1
        Log.debug(
            "New draft version=$newVersionNumber for estimation $estimationId, " +
                "clonedFrom=${latestSubmitted?.versionNumber}"
        )

        val draft = DraftEstimationVersion().apply {
            this.estimation = estimation
            this.versionNumber = newVersionNumber
        }

        if (latestSubmitted != null) {
            cloneFromSubmitted(latestSubmitted, draft)
        } else {
            // No submitted version to inherit from: seed the installation's
            // standard effort drivers so a first estimate does not start empty.
            // Deliberately NOT done on the clone path — appending the template
            // there would silently mutate a continuing estimate.
            seedStandardDrivers(draft)
        }

        draftRepository.persist(draft)

        auditLogService.log(
            null, "DraftEstimationVersion", draft.id,
            "CREATE", "version=$newVersionNumber, clonedFrom=${latestSubmitted?.versionNumber}"
        )

        Log.info("Created draft ${draft.id} (version=$newVersionNumber) for estimation $estimationId")
        return draft
    }

    @Transactional
    fun importMerlinDraft(estimationId: UUID, input: InputStream): DraftEstimationVersion {
        Log.info("Importing Merlin draft for estimation $estimationId")
        val estimation = requireEstimationWithoutDraft(estimationId)
        val latestSubmitted = submittedRepository.findLatestByEstimationId(estimationId)
        val newVersionNumber = (latestSubmitted?.versionNumber ?: 0) + 1
        val draft = try {
            merlinImporter.import(input, estimation, newVersionNumber)
        } catch (e: IllegalArgumentException) {
            Log.error("Merlin import failed for estimation $estimationId: ${e.message}")
            throw WebApplicationException("Invalid Merlin file: ${e.message}", e, Response.Status.BAD_REQUEST)
        }
        draftRepository.persist(draft)
        auditLogService.log(
            null, "DraftEstimationVersion", draft.id, "IMPORT_MERLIN", "version=$newVersionNumber"
        )
        Log.info("Imported Merlin draft ${draft.id} (version=$newVersionNumber) for estimation $estimationId")
        return draft
    }

    /**
     * Imports an xlsx workbook this application exported as a new draft (task-183).
     *
     * Mirrors [importMerlinDraft] with one deliberate difference: it takes the
     * uploaded file's [Path] rather than an `InputStream`, because the workbook
     * has to be read TWICE — once to decide which method it was written for, and
     * once to import it. An `InputStream` is single-pass, so peeking would leave
     * the importer an exhausted stream.
     */
    @Transactional
    fun importXlsxDraft(estimationId: UUID, file: Path): DraftEstimationVersion {
        Log.info("Importing xlsx draft for estimation $estimationId")
        val estimation = requireEstimationWithoutDraft(estimationId)
        requireWorkbookMatchesMethod(file, estimation)
        val latestSubmitted = submittedRepository.findLatestByEstimationId(estimationId)
        val newVersionNumber = (latestSubmitted?.versionNumber ?: 0) + 1
        val draft = readingWorkbook(estimationId) {
            file.inputStream().use { excelImporter.import(it, estimation, newVersionNumber) }
        }
        draftRepository.persist(draft)
        auditLogService.log(
            null, "DraftEstimationVersion", draft.id, "IMPORT_XLSX", "version=$newVersionNumber"
        )
        Log.info("Imported xlsx draft ${draft.id} (version=$newVersionNumber) for estimation $estimationId")
        return draft
    }

    /**
     * Refuses a workbook written for a different estimation method.
     *
     * [ExcelImporter] shapes its column block from `estimation.method`, so a
     * bucket+sampled workbook (five method columns) read as PERT (three) would
     * take every trailing value from the wrong offset and import numbers nobody
     * entered — silently. The file says which method wrote it: the exporter
     * writes the `Eimer` sheet on method alone, so its presence is an exact
     * mirror of this check.
     *
     * 422 rather than 400: the upload is a perfectly well-formed workbook, it
     * just cannot be processed into THIS estimation. The frontend tells the two
     * failures apart by status, so they must not share one.
     */
    private fun requireWorkbookMatchesMethod(file: Path, estimation: Estimation) {
        val workbookMethod = readingWorkbook(estimation.id) { methodOf(file) }
        if (workbookMethod != estimation.method) {
            Log.warn(
                "xlsx import rejected for estimation ${estimation.id}: " +
                    "workbook is $workbookMethod, estimation is ${estimation.method}"
            )
            throw WebApplicationException(
                "This workbook was exported from a $workbookMethod estimation, " +
                    "but this estimation uses ${estimation.method}.",
                UNPROCESSABLE_CONTENT
            )
        }
    }

    /** Which method wrote this workbook — only a bucket+sampled export has `Eimer`. */
    private fun methodOf(file: Path): EstimationMethod =
        file.inputStream().use { stream ->
            XSSFWorkbook(stream).use { workbook ->
                if (workbook.getSheet(ExcelGermanLabels.Sheets.BUCKETS) != null) {
                    EstimationMethod.BUCKET_SAMPLED_PERT
                } else {
                    EstimationMethod.THREE_POINT_PERT
                }
            }
        }

    /**
     * Runs a workbook read, turning "this is not a readable .xlsx" into `400`.
     *
     * The single place that names POI's failure set, so the peek and the import
     * cannot disagree about what counts as unreadable. `NotOfficeXmlFileException`
     * — POI's "not an OOXML file at all" — extends [IllegalArgumentException],
     * which is easy to miss: leave it out and an unreadable upload escapes as a
     * `500` instead of the documented `400`. Anything outside that set is a real
     * fault and is rethrown untouched, so this never becomes a blanket catch.
     */
    private inline fun <T> readingWorkbook(estimationId: UUID?, read: () -> T): T =
        runCatching(read).getOrElse { e ->
            when (e) {
                is IOException, is POIXMLException, is OpenXML4JRuntimeException, is IllegalArgumentException ->
                    throw invalidWorkbook(estimationId, e)
                else -> throw e
            }
        }

    private fun invalidWorkbook(estimationId: UUID?, e: Throwable): WebApplicationException {
        Log.error("xlsx import failed for estimation $estimationId: ${e.message}")
        return WebApplicationException("Invalid Excel workbook: ${e.message}", e, Response.Status.BAD_REQUEST)
    }

    // Loads the estimation for a new draft, enforcing the "one draft at a time"
    // rule (404 if missing, 409 if a draft already exists). Logs the rejection so
    // a failed import is diagnosable server-side (task-131 follow-up). Shared by
    // the Merlin and xlsx importers, so the message names neither (task-183).
    private fun requireEstimationWithoutDraft(estimationId: UUID): Estimation {
        val estimation = estimationRepository.findById(estimationId)
            ?: run {
                Log.warn("Draft import rejected: estimation $estimationId not found")
                throw WebApplicationException("Estimation not found: $estimationId", Response.Status.NOT_FOUND)
            }
        if (draftRepository.findByEstimationId(estimationId) != null) {
            Log.warn("Draft import rejected: a draft already exists for estimation $estimationId")
            throw WebApplicationException(
                "A draft already exists for estimation $estimationId",
                Response.Status.CONFLICT
            )
        }
        return estimation
    }

    @Transactional
    fun submitDraft(estimationId: UUID): SubmittedEstimationVersion {
        Log.info("Submitting draft for estimation $estimationId")
        estimationRepository.findById(estimationId)
            ?: run {
                Log.error("Cannot submit draft: estimation $estimationId not found")
                throw WebApplicationException("Estimation not found: $estimationId", Response.Status.NOT_FOUND)
            }

        val draft = draftRepository.findByEstimationId(estimationId)
            ?: run {
                Log.error("Cannot submit draft: no draft found for estimation $estimationId")
                throw WebApplicationException("No draft found for estimation $estimationId", Response.Status.NOT_FOUND)
            }

        val submitted = snapshotDraft(draft)

        submittedRepository.persist(submitted)
        draftRepository.delete(draft)

        auditLogService.log(
            null, "SubmittedEstimationVersion", submitted.id,
            "SUBMIT", "version=${submitted.versionNumber}"
        )

        val leafCount = countLeaves(submitted.roots)
        Log.info(
            "Submitted estimation $estimationId as version ${submitted.versionNumber} " +
                "(method=${draft.estimation?.method}, leaves=$leafCount)"
        )
        return submitted
    }

    fun snapshotDraft(draft: DraftEstimationVersion): SubmittedEstimationVersion {
        val calculated = draftVersionMapper.toDomain(draft).calculate()

        val submitted = SubmittedEstimationVersion().apply {
            this.estimation = draft.estimation
            this.versionNumber = draft.versionNumber
            this.totalEffort = calculated.totalEffort
            this.notes = draft.notes
            this.submittedAt = Instant.now()
            this.createdAt = draft.createdAt ?: Instant.now()
        }

        submitted.dailyRate = draft.dailyRate
        submitted.stdDevFactor = draft.stdDevFactor
        submitted.salesSurcharge = draft.salesSurcharge

        // Schedule inputs (task-156). A snapshot without them renders an empty
        // Gantt where the draft rendered a full one.
        submitted.teamFte = draft.teamFte
        draft.scheduleDependencies.forEach { d ->
            submitted.scheduleDependencies.add(SubmittedScheduleDependency().apply {
                fromLogicalId = d.fromLogicalId
                toLogicalId = d.toLogicalId
                version = submitted
            })
            Log.debug("Snapshotting schedule edge ${d.fromLogicalId} -> ${d.toLogicalId}")
        }
        val edgeCount = draft.scheduleDependencies.size
        Log.info("Schedule snapshot: draft=${draft.id} teamFte=${draft.teamFte} edges=$edgeCount")

        draft.effortDrivers.forEach { d ->
            submitted.effortDrivers.add(SubmittedEffortDriver().apply {
                description = d.description
                factor = d.factor
                comment = d.comment
                version = submitted
            })
        }

        draft.phases.forEach { p ->
            submitted.phases.add(SubmittedProjectPhase().apply {
                name = p.name
                abbreviation = p.abbreviation
                durationWeeks = p.durationWeeks
                // A field added to the DTO but not here is silently dropped on
                // submit, and only the snapshot shows it (task-177).
                durationMode = p.durationMode
                version = submitted
            })
        }

        // Walk the calculated domain tree recursively and mirror it as
        // SubmittedEstimationNode entities. Group nodes carry the accumulated
        // values from the domain group (EstimationGroup accumulates via
        // sumOf over its children). Leaf nodes carry per-leaf calculated
        // values. The DraftEstimationNode tree gives us per-leaf phase
        // abbreviations and per-leaf descriptions; the domain tree gives us
        // the calculated numbers.
        val draftNodesById = indexDraftNodes(draft.roots)
        calculated.roots.forEachIndexed { idx, root ->
            submitted.roots.add(buildSubmittedNode(submitted, draftNodesById, root, null, idx))
        }

        draft.additionalCosts.forEach { c ->
            submitted.additionalCosts.add(SubmittedAdditionalCost().apply {
                description = c.description
                amount = c.amount
                type = c.type
                amountPerWeek = c.amountPerWeek
                phaseAbbreviation = c.phase?.abbreviation
                version = submitted
            })
        }

        return submitted
    }

    private fun countLeaves(nodes: List<SubmittedEstimationNode>): Int =
        nodes.sumOf { if (it is SubmittedGroupNode) countLeaves(it.children) else 1 }

    private fun indexDraftNodes(roots: List<DraftEstimationNode>): Map<String, DraftEstimationNode> {
        val byId = mutableMapOf<String, DraftEstimationNode>()
        fun index(node: DraftEstimationNode) {
            byId[node.logicalId.toString()] = node
            node.children.forEach(::index)
        }
        roots.forEach(::index)
        return byId
    }

    private fun buildSubmittedNode(
        submitted: SubmittedEstimationVersion,
        draftNodesById: Map<String, DraftEstimationNode>,
        domainNode: EstimationNode,
        parentNode: SubmittedEstimationNode?,
        pos: Int
    ): SubmittedEstimationNode {
        val draftNode = draftNodesById[domainNode.logicalId]
        val node: SubmittedEstimationNode = when (domainNode) {
            is EstimationGroup -> SubmittedGroupNode().apply { title = domainNode.title }
            is TimeRelativeEstimationItem -> SubmittedTimeRelativeItemNode().apply { unit = domainNode.unit }
            is BucketedEstimationItem -> SubmittedBucketedItemNode().apply {
                bucket = submitted.estimation?.buckets?.find { it.id?.toString() == domainNode.bucketId }
                isSample = domainNode.isSample
            }
            is FixedEstimationItem -> SubmittedFixedItemNode()
            // EstimationItem is now abstract (task-113): the when is no longer
            // exhaustive, so fail loudly on a leaf type this side can't persist.
            else -> error("Unknown estimation node type: ${domainNode::class.simpleName}")
        }
        node.apply {
            logicalId = UUID.fromString(domainNode.logicalId)
            position = pos
            version = submitted
            parent = parentNode
            mean = domainNode.mean
            variance = domainNode.variance
            riskSurcharge = domainNode.riskSurcharge
            driverSurcharge = domainNode.driverSurcharge
            offerPT = domainNode.offerPT
            cost = domainNode.cost
            offerPrice = domainNode.offerPrice
            if (domainNode is EstimationItem) {
                description = domainNode.description
                code = domainNode.code
                minEffort = domainNode.minEffort
                expectedEffort = domainNode.expectedEffort
                maxEffort = domainNode.maxEffort
                assumptions = domainNode.assumptions
                phaseAbbreviation = domainNode.phase?.abbreviation
                    ?: draftNode?.phase?.abbreviation
            }
            if (domainNode is BucketedEstimationItem) {
                // A bucketed sample stores its optimistic/likely/pessimistic
                // triple in the shared min/expected/max columns (non-samples
                // leave them NULL and carry only the bucket-averaged neutrals).
                minEffort = domainNode.optimistic
                expectedEffort = domainNode.likely
                maxEffort = domainNode.pessimistic
            }
        }
        if (domainNode is EstimationGroup) {
            domainNode.children.forEachIndexed { idx, child ->
                node.children.add(buildSubmittedNode(submitted, draftNodesById, child, node, idx))
            }
        }
        return node
    }

    @Transactional
    fun deleteDraft(estimationId: UUID) {
        Log.info("Deleting draft for estimation $estimationId")
        val draft = draftRepository.findByEstimationId(estimationId)
            ?: run {
                Log.error("Cannot delete draft: no draft found for estimation $estimationId")
                throw WebApplicationException("No draft found for estimation $estimationId", Response.Status.NOT_FOUND)
            }
        draftRepository.delete(draft)
    }

    private fun seedStandardDrivers(draft: DraftEstimationVersion) {
        val standard = systemSettingsService.standardDrivers()
        standard.forEach { template ->
            draft.effortDrivers.add(DraftEffortDriver().apply {
                this.version = draft
                this.description = template.description
                this.factor = template.factor
                this.comment = template.comment
            })
        }
        if (standard.isNotEmpty()) {
            Log.debug("Seeded ${standard.size} standard effort driver(s) into the new draft")
        }
    }

    private fun cloneFromSubmitted(source: SubmittedEstimationVersion, target: DraftEstimationVersion) {
        target.dailyRate = source.dailyRate
        target.stdDevFactor = source.stdDevFactor
        target.salesSurcharge = source.salesSurcharge

        // The THIRD schedule-carrying path (task-156): a draft cloned from a
        // submitted version must inherit its schedule, or continuing an
        // estimate silently starts over with no dependencies.
        target.teamFte = source.teamFte
        source.scheduleDependencies.forEach { d ->
            target.scheduleDependencies.add(DraftScheduleDependency().apply {
                fromLogicalId = d.fromLogicalId
                toLogicalId = d.toLogicalId
                version = target
            })
        }
        val clonedEdges = source.scheduleDependencies.size
        Log.debug("Cloned schedule: v${source.versionNumber} teamFte=${source.teamFte} edges=$clonedEdges")

        source.effortDrivers.forEach { d ->
            target.effortDrivers.add(DraftEffortDriver().apply {
                description = d.description
                factor = d.factor
                comment = d.comment
                version = target
            })
        }

        val phaseMapping = mutableMapOf<String, DraftProjectPhase>()
        source.phases.forEach { p ->
            val draftPhase = DraftProjectPhase().apply {
                name = p.name
                abbreviation = p.abbreviation
                durationWeeks = p.durationWeeks
                durationMode = p.durationMode
                version = target
            }
            phaseMapping[p.abbreviation] = draftPhase
            target.phases.add(draftPhase)
        }

        source.roots.forEachIndexed { idx, root ->
            target.roots.add(cloneSubmittedNode(target, phaseMapping, root, null, idx))
        }

        source.additionalCosts.forEach { c ->
            target.additionalCosts.add(DraftAdditionalCost().apply {
                description = c.description
                amount = c.amount
                type = c.type
                amountPerWeek = c.amountPerWeek
                phase = c.phaseAbbreviation?.let { phaseMapping[it] }
                version = target
            })
        }
    }

    private fun cloneSubmittedNode(
        target: DraftEstimationVersion,
        phaseMapping: Map<String, DraftProjectPhase>,
        submittedNode: SubmittedEstimationNode,
        parentDraft: DraftEstimationNode?,
        pos: Int
    ): DraftEstimationNode {
        val draftNode: DraftEstimationNode = when (submittedNode) {
            is SubmittedGroupNode -> DraftGroupNode().apply { title = submittedNode.title }
            is SubmittedTimeRelativeItemNode -> DraftTimeRelativeItemNode().apply { unit = submittedNode.unit }
            is SubmittedBucketedItemNode -> DraftBucketedItemNode().apply {
                bucket = submittedNode.bucket
                isSample = submittedNode.isSample
            }
            is SubmittedFixedItemNode -> DraftFixedItemNode()
            else -> error("Unknown submitted node type: ${submittedNode::class.simpleName}")
        }
        draftNode.apply {
            logicalId = submittedNode.logicalId
            position = pos
            version = target
            parent = parentDraft
            if (submittedNode !is SubmittedGroupNode) {
                description = submittedNode.description
                code = submittedNode.code
                minEffort = submittedNode.minEffort
                expectedEffort = submittedNode.expectedEffort
                maxEffort = submittedNode.maxEffort
                assumptions = submittedNode.assumptions
                phase = submittedNode.phaseAbbreviation?.let { phaseMapping[it] }
            }
        }
        submittedNode.children.forEachIndexed { idx, child ->
            draftNode.children.add(cloneSubmittedNode(target, phaseMapping, child, draftNode, idx))
        }
        return draftNode
    }
}
