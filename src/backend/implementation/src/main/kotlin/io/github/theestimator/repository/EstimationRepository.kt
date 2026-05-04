package io.github.theestimator.repository

import io.github.theestimator.domain.Estimation
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class EstimationRepository : PanacheRepositoryBase<Estimation, UUID> {

    fun findByProjectId(projectId: UUID): List<Estimation> =
        list("project.id", projectId)
}
