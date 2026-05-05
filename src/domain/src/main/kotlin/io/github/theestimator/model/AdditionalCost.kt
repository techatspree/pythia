package io.github.theestimator.model

@DomainEntity
class AdditionalCost : BaseDomain() {
    var description: String? = null
    var amount: Double = 0.0
    var type: AdditionalCostType = AdditionalCostType.ONE_TIME
    var amountPerWeek: Double? = null
    var phase: ProjectPhase? = null
    var version: EstimationVersion? = null
}
