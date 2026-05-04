package io.github.theestimator.repository

import io.github.theestimator.domain.EstimationVersion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class EstimationVersionRepository : PanacheRepositoryBase<EstimationVersion, UUID> {

    fun findByEstimationId(estimationId: UUID): List<EstimationVersion> =
        list("estimation.id", estimationId)

    fun findLatestByEstimationId(estimationId: UUID): EstimationVersion? =
        find("estimation.id = ?1 ORDER BY versionNumber DESC", estimationId).firstResult()
}
