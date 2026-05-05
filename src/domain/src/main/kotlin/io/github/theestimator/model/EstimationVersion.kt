package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
class EstimationVersion(
    id: UUID? = null,
    createdAt: Instant? = null
) : BaseDomain(id, createdAt) {
    var versionNumber: Int? = null
    var status: EstimationVersionStatus = EstimationVersionStatus.DRAFT
    var createdBy: User? = null
    var totalEffort: Double? = null
    var notes: String? = null
    var estimation: Estimation? = null
    var parameters: MutableList<EstimationParameter> = mutableListOf()
    var effortDrivers: MutableList<EffortDriver> = mutableListOf()
    var phases: MutableList<ProjectPhase> = mutableListOf()
    var additionalCosts: MutableList<AdditionalCost> = mutableListOf()
    var itemGroups: MutableList<EstimationItemGroup> = mutableListOf()
}
