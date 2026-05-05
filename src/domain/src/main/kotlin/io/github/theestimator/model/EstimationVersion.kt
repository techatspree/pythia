package io.github.theestimator.model

@DomainEntity
class EstimationVersion : BaseDomain() {
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
