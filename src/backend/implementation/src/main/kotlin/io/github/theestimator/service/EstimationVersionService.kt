package io.github.theestimator.service

import io.github.theestimator.domain.draft.*
import io.github.theestimator.domain.submitted.*
import io.github.theestimator.model.EstimationGroup
import io.github.theestimator.model.EstimationItem
import io.github.theestimator.model.EstimationNode
import io.github.theestimator.model.EstimationVersion
import io.github.theestimator.model.FixedEstimationItem
import io.github.theestimator.model.TimeRelativeEstimationItem
import io.github.theestimator.repository.DraftEstimationVersionRepository
import io.github.theestimator.repository.EstimationRepository
import io.github.theestimator.repository.SubmittedEstimationVersionRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import java.time.Instant
import java.util.UUID

@ApplicationScoped
class EstimationVersionService(
    private val draftRepository: DraftEstimationVersionRepository,
    private val submittedRepository: SubmittedEstimationVersionRepository,
    private val estimationRepository: EstimationRepository,
    private val draftVersionMapper: DraftVersionMapper,
    private val auditLogService: AuditLogService
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
        val estimation = estimationRepository.findById(estimationId)
            ?: throw WebApplicationException("Estimation not found: $estimationId", Response.Status.NOT_FOUND)

        if (draftRepository.findByEstimationId(estimationId) != null) {
            throw WebApplicationException("A draft already exists for estimation $estimationId", Response.Status.CONFLICT)
        }

        val latestSubmitted = submittedRepository.findLatestByEstimationId(estimationId)
        val newVersionNumber = (latestSubmitted?.versionNumber ?: 0) + 1

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

        return draft
    }

    @Transactional
    fun submitDraft(estimationId: UUID): SubmittedEstimationVersion {
        estimationRepository.findById(estimationId)
            ?: throw WebApplicationException("Estimation not found: $estimationId", Response.Status.NOT_FOUND)

        val draft = draftRepository.findByEstimationId(estimationId)
            ?: throw WebApplicationException("No draft found for estimation $estimationId", Response.Status.NOT_FOUND)

        val submitted = snapshotDraft(draft)

        submittedRepository.persist(submitted)
        draftRepository.delete(draft)

        auditLogService.log(
            null, "SubmittedEstimationVersion", submitted.id,
            "SUBMIT", "version=${submitted.versionNumber}"
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

        draft.parameters.forEach { p ->
            submitted.parameters.add(SubmittedEstimationParameter().apply {
                name = p.name
                value = p.value
                comment = p.comment
                version = submitted
            })
        }

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
        val draftNodesById = mutableMapOf<String, DraftEstimationNode>()
        fun indexDraft(node: DraftEstimationNode) {
            draftNodesById[node.logicalId.toString()] = node
            node.children.forEach(::indexDraft)
        }
        draft.roots.forEach(::indexDraft)

        fun buildSubmitted(domainNode: EstimationNode, parentNode: SubmittedEstimationNode?, pos: Int): SubmittedEstimationNode {
            val draftNode = draftNodesById[domainNode.logicalId]
            val node: SubmittedEstimationNode = when (domainNode) {
                is EstimationGroup -> SubmittedGroupNode().apply { title = domainNode.title }
                is TimeRelativeEstimationItem -> SubmittedTimeRelativeItemNode().apply { unit = domainNode.unit }
                is FixedEstimationItem -> SubmittedFixedItemNode()
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
            }
            if (domainNode is EstimationGroup) {
                domainNode.children.forEachIndexed { idx, child ->
                    node.children.add(buildSubmitted(child, node, idx))
                }
            }
            return node
        }

        calculated.roots.forEachIndexed { idx, root ->
            submitted.roots.add(buildSubmitted(root, null, idx))
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

    @Transactional
    fun deleteDraft(estimationId: UUID) {
        val draft = draftRepository.findByEstimationId(estimationId)
            ?: throw WebApplicationException("No draft found for estimation $estimationId", Response.Status.NOT_FOUND)
        draftRepository.delete(draft)
    }

    private fun cloneFromSubmitted(source: SubmittedEstimationVersion, target: DraftEstimationVersion) {
        source.parameters.forEach { p ->
            target.parameters.add(DraftEstimationParameter().apply {
                name = p.name
                value = p.value
                comment = p.comment
                version = target
            })
        }

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

        fun cloneNode(submittedNode: SubmittedEstimationNode, parentDraft: DraftEstimationNode?, pos: Int): DraftEstimationNode {
            val draftNode: DraftEstimationNode = when (submittedNode) {
                is SubmittedGroupNode -> DraftGroupNode().apply { title = submittedNode.title }
                is SubmittedTimeRelativeItemNode -> DraftTimeRelativeItemNode().apply { unit = submittedNode.unit }
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
                draftNode.children.add(cloneNode(child, draftNode, idx))
            }
            return draftNode
        }

        source.roots.forEachIndexed { idx, root ->
            target.roots.add(cloneNode(root, null, idx))
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
}
