package io.github.theestimator.model

@DomainEntity
class EstimationParameter : BaseDomain() {
    var name: String? = null
    var value: Double = 0.0
    var comment: String? = null
    var version: EstimationVersion? = null
}
