package io.pythia.repository

import io.pythia.domain.submitted.SubmittedEstimationVersion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class SubmittedEstimationVersionRepository : PanacheRepositoryBase<SubmittedEstimationVersion, UUID> {

    fun findByEstimationId(estimationId: UUID): List<SubmittedEstimationVersion> =
        list("estimation.id = ?1 ORDER BY versionNumber DESC", estimationId)

    fun findByEstimationIdAndVersionNumber(estimationId: UUID, versionNumber: Int): SubmittedEstimationVersion? =
        find("estimation.id = ?1 AND versionNumber = ?2", estimationId, versionNumber).firstResult()

    fun findLatestByEstimationId(estimationId: UUID): SubmittedEstimationVersion? =
        find("estimation.id = ?1 ORDER BY versionNumber DESC", estimationId).firstResult()
}
