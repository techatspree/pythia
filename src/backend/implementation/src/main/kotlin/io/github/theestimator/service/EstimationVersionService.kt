package io.github.theestimator.service

import io.github.theestimator.domain.draft.*
import io.github.theestimator.domain.submitted.*
import io.github.theestimator.model.EstimationVersion
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
        val estimation = estimationRepository.findById(estimationId)
            ?: throw WebApplicationException("Estimation not found: $estimationId", Response.Status.NOT_FOUND)

        val draft = draftRepository.findByEstimationId(estimationId)
            ?: throw WebApplicationException("No draft found for estimation $estimationId", Response.Status.NOT_FOUND)

        val calculated = draftVersionMapper.toDomain(draft).calculate()

        val submitted = SubmittedEstimationVersion().apply {
            this.estimation = estimation
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

        val itemResultMap = calculated.itemGroups.flatMap { it.items }.associateBy { it.logicalId }

        draft.itemGroups.forEach { group ->
            val submittedGroup = SubmittedEstimationItemGroup().apply {
                logicalId = group.logicalId
                title = group.title
                phaseAbbreviation = group.phase?.abbreviation
                version = submitted
            }
            submitted.itemGroups.add(submittedGroup)

            group.items.forEach { item ->
                val calc = itemResultMap[item.logicalId.toString()]
                val submittedItem = when (item) {
                    is DraftTimeRelativeEstimationItem -> SubmittedTimeRelativeEstimationItem().apply { unit = item.unit }
                    else -> SubmittedFixedEstimationItem()
                }
                submittedItem.apply {
                    logicalId = item.logicalId
                    description = item.description
                    code = item.code
                    minEffort = item.minEffort ?: 0.0
                    expectedEffort = item.expectedEffort ?: 0.0
                    maxEffort = item.maxEffort ?: 0.0
                    mean = calc?.mean ?: 0.0
                    variance = calc?.variance ?: 0.0
                    riskSurcharge = calc?.riskSurcharge ?: 0.0
                    driverSurcharge = calc?.driverSurcharge ?: 0.0
                    offerPT = calc?.offerPT ?: 0.0
                    cost = calc?.cost ?: 0.0
                    offerPrice = calc?.offerPrice ?: 0.0
                    assumptions = item.assumptions
                    this.group = submittedGroup
                }
                submittedGroup.items.add(submittedItem)
            }
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

        submittedRepository.persist(submitted)
        draftRepository.delete(draft)

        auditLogService.log(
            null, "SubmittedEstimationVersion", submitted.id,
            "SUBMIT", "version=${submitted.versionNumber}"
        )

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

        source.itemGroups.forEach { group ->
            val draftGroup = DraftEstimationItemGroup().apply {
                logicalId = group.logicalId
                title = group.title
                phase = group.phaseAbbreviation?.let { phaseMapping[it] }
                version = target
            }
            target.itemGroups.add(draftGroup)

            group.items.forEach { item ->
                val draftItem = when (item) {
                    is SubmittedTimeRelativeEstimationItem -> DraftTimeRelativeEstimationItem().apply { unit = item.unit }
                    else -> DraftFixedEstimationItem()
                }
                draftItem.apply {
                    logicalId = item.logicalId
                    description = item.description
                    code = item.code
                    minEffort = item.minEffort
                    expectedEffort = item.expectedEffort
                    maxEffort = item.maxEffort
                    assumptions = item.assumptions
                    phase = group.phaseAbbreviation?.let { phaseMapping[it] }
                    this.group = draftGroup
                }
                draftGroup.items.add(draftItem)
            }
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
