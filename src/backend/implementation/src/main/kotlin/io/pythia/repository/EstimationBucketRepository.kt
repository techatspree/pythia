package io.pythia.repository

import io.pythia.domain.EstimationBucket
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class EstimationBucketRepository : PanacheRepositoryBase<EstimationBucket, UUID> {

    fun findByEstimationId(estimationId: UUID): List<EstimationBucket> =
        list("estimation.id", estimationId)
}
