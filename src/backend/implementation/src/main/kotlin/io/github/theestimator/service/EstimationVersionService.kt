package io.github.theestimator.service

import io.github.theestimator.domain.*
import io.github.theestimator.repository.EstimationRepository
import io.github.theestimator.repository.EstimationVersionRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.io.InputStream
import java.util.UUID

@ApplicationScoped
class EstimationVersionService(
    private val versionRepository: EstimationVersionRepository,
    private val estimationRepository: EstimationRepository,
    private val calculator: EstimationCalculator,
    private val importer: ExcelImporter,
    private val auditLogService: AuditLogService
) {

    fun findById(id: UUID): EstimationVersion? = versionRepository.findById(id)

    fun findByEstimationId(estimationId: UUID): List<EstimationVersion> =
        versionRepository.findByEstimationId(estimationId)

    @Transactional
    fun createNewVersion(estimationId: UUID, createdBy: User? = null): EstimationVersion {
        val estimation = estimationRepository.findById(estimationId)
            ?: throw IllegalArgumentException("Estimation not found: $estimationId")

        val latest = versionRepository.findLatestByEstimationId(estimationId)
        val newVersionNumber = (latest?.versionNumber ?: 0) + 1

        val newVersion = EstimationVersion().apply {
            this.estimation = estimation
            this.versionNumber = newVersionNumber
            this.createdBy = createdBy
            this.status = EstimationVersionStatus.DRAFT
        }

        if (latest != null) {
            deepClone(latest, newVersion)
        }

        calculator.calculate(newVersion)
        versionRepository.persist(newVersion)

        auditLogService.log(
            createdBy?.id, "EstimationVersion", newVersion.id,
            "CREATE", "version=$newVersionNumber, clonedFrom=${latest?.versionNumber}"
        )

        return newVersion
    }

    @Transactional
    fun importFromExcel(estimationId: UUID, inputStream: InputStream, createdBy: User? = null): EstimationVersion {
        val estimation = estimationRepository.findById(estimationId)
            ?: throw IllegalArgumentException("Estimation not found: $estimationId")

        val latest = versionRepository.findLatestByEstimationId(estimationId)
        val newVersionNumber = (latest?.versionNumber ?: 0) + 1

        val version = importer.import(inputStream, estimation, newVersionNumber, createdBy)
        calculator.calculate(version)
        versionRepository.persist(version)

        auditLogService.log(
            createdBy?.id, "EstimationVersion", version.id,
            "IMPORT", "version=$newVersionNumber"
        )

        return version
    }

    @Transactional
    fun submit(versionId: UUID, submittedBy: User? = null): EstimationVersion {
        val version = versionRepository.findById(versionId)
            ?: throw IllegalArgumentException("Version not found: $versionId")

        if (version.status != EstimationVersionStatus.DRAFT) {
            throw IllegalStateException("Cannot submit version in status ${version.status}")
        }

        version.status = EstimationVersionStatus.SUBMITTED
        version.estimation?.currentVersion = version

        auditLogService.log(
            submittedBy?.id, "EstimationVersion", version.id,
            "SUBMIT", "version=${version.versionNumber}"
        )

        return version
    }

    fun ensureDraft(version: EstimationVersion) {
        if (version.status != EstimationVersionStatus.DRAFT) {
            throw IllegalStateException("Cannot edit version in status ${version.status}")
        }
    }

    private fun deepClone(source: EstimationVersion, target: EstimationVersion) {
        source.parameters.forEach { param ->
            target.parameters.add(EstimationParameter().apply {
                name = param.name
                value = param.value
                comment = param.comment
                version = target
            })
        }

        source.effortDrivers.forEach { driver ->
            target.effortDrivers.add(EffortDriver().apply {
                description = driver.description
                factor = driver.factor
                comment = driver.comment
                version = target
            })
        }

        val phaseMapping = mutableMapOf<UUID?, ProjectPhase>()
        source.phases.forEach { phase ->
            val newPhase = ProjectPhase().apply {
                name = phase.name
                abbreviation = phase.abbreviation
                durationWeeks = phase.durationWeeks
                version = target
            }
            phaseMapping[phase.id] = newPhase
            target.phases.add(newPhase)
        }

        source.itemGroups.forEach { group ->
            val newPhase = group.phase?.id?.let { phaseMapping[it] }
            val newGroup = EstimationItemGroup().apply {
                logicalId = group.logicalId
                title = group.title
                phase = newPhase
                version = target
            }
            target.itemGroups.add(newGroup)

            group.items.forEach { item ->
                val newItem = when (item) {
                    is TimeRelativeEstimationItem -> TimeRelativeEstimationItem().apply { unit = item.unit }
                    is FixedEstimationItem -> FixedEstimationItem()
                    else -> FixedEstimationItem()
                }
                newItem.apply {
                    logicalId = item.logicalId
                    description = item.description
                    code = item.code
                    minEffort = item.minEffort
                    expectedEffort = item.expectedEffort
                    maxEffort = item.maxEffort
                    assumptions = item.assumptions
                    phase = item.phase?.id?.let { phaseMapping[it] }
                    this.group = newGroup
                }
                newGroup.items.add(newItem)
            }
        }

        source.additionalCosts.forEach { cost ->
            target.additionalCosts.add(AdditionalCost().apply {
                description = cost.description
                amount = cost.amount
                type = cost.type
                amountPerWeek = cost.amountPerWeek
                phase = cost.phase?.id?.let { phaseMapping[it] }
                version = target
            })
        }
    }
}
