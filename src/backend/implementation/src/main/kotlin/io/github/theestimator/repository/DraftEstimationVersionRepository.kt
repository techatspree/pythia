package io.github.theestimator.repository

import io.github.theestimator.domain.draft.DraftEstimationVersion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class DraftEstimationVersionRepository : PanacheRepositoryBase<DraftEstimationVersion, UUID> {

    fun findByEstimationId(estimationId: UUID): DraftEstimationVersion? =
        find("estimation.id", estimationId).firstResult()
}
