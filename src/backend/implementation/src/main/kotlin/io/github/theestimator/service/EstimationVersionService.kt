package io.github.theestimator.service

import io.github.theestimator.domain.Estimation
import io.github.theestimator.domain.draft.DraftAdditionalCost
import io.github.theestimator.domain.draft.DraftEffortDriver
import io.github.theestimator.domain.draft.DraftEstimationNode
import io.github.theestimator.domain.draft.DraftEstimationVersion
import io.github.theestimator.domain.draft.DraftBucketedItemNode
import io.github.theestimator.domain.draft.DraftFixedItemNode
import io.github.theestimator.domain.draft.DraftGroupNode
import io.github.theestimator.domain.draft.DraftProjectPhase
import io.github.theestimator.domain.draft.DraftTimeRelativeItemNode
import io.github.theestimator.domain.submitted.SubmittedAdditionalCost
import io.github.theestimator.domain.submitted.SubmittedEffortDriver
import io.github.theestimator.domain.submitted.SubmittedEstimationNode
import io.github.theestimator.domain.submitted.SubmittedEstimationVersion
import io.github.theestimator.domain.submitted.SubmittedBucketedItemNode
import io.github.theestimator.domain.submitted.SubmittedFixedItemNode
import io.github.theestimator.domain.submitted.SubmittedGroupNode
import io.github.theestimator.domain.submitted.SubmittedProjectPhase
import io.github.theestimator.domain.submitted.SubmittedTimeRelativeItemNode
import io.github.theestimator.method.bucketsampled.BucketedEstimationItem
import io.github.theestimator.method.threepoint.FixedEstimationItem
import io.github.theestimator.method.threepoint.TimeRelativeEstimationItem
import io.github.theestimator.model.EstimationGroup
import io.github.theestimator.model.EstimationItem
import io.github.theestimator.model.EstimationNode
import io.github.theestimator.model.EstimationVersion
import io.github.theestimator.repository.DraftEstimationVersionRepository
import io.github.theestimator.repository.EstimationRepository
import io.github.theestimator.repository.SubmittedEstimationVersionRepository
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import java.io.InputStream
import java.time.Instant
import java.util.UUID

// The snapshot/clone tree builders are split into focused private helpers to
// keep each method's complexity low; that deliberately raises the class's
// function count past the TooManyFunctions threshold for one cohesive service.
@Suppress("TooManyFunctions")
@ApplicationScoped
class EstimationVersionService(
    private val draftRepository: DraftEstimationVersionRepository,
    private val submittedRepository: SubmittedEstimationVersionRepository,
    private val estimationRepository: EstimationRepository,
    private val draftVersionMapper: DraftVersionMapper,
    private val auditLogService: AuditLogService,
    private val merlinImporter: MerlinImporter
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

    // Loads the estimation for a new draft, enforcing the "one draft at a time"
    // rule (404 if missing, 409 if a draft already exists). Logs the rejection so
    // a failed Merlin import is diagnosable server-side (task-131 follow-up).
    private fun requireEstimationWithoutDraft(estimationId: UUID): Estimation {
        val estimation = estimationRepository.findById(estimationId)
            ?: run {
                Log.warn("Merlin import rejected: estimation $estimationId not found")
                throw WebApplicationException("Estimation not found: $estimationId", Response.Status.NOT_FOUND)
            }
        if (draftRepository.findByEstimationId(estimationId) != null) {
            Log.warn("Merlin import rejected: a draft already exists for estimation $estimationId")
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

    private fun cloneFromSubmitted(source: SubmittedEstimationVersion, target: DraftEstimationVersion) {
        target.dailyRate = source.dailyRate
        target.stdDevFactor = source.stdDevFactor
        target.salesSurcharge = source.salesSurcharge

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
