package io.github.theestimator.model

@DomainEntity
class EffortDriver : BaseDomain() {
    var description: String? = null
    var factor: Double = 0.0
    var comment: String? = null
    var version: EstimationVersion? = null
}
