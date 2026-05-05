package io.github.theestimator.model

@DomainEntity
class ProjectPhase : BaseDomain() {
    var name: String? = null
    var abbreviation: String? = null
    var durationWeeks: Double? = null
    var version: EstimationVersion? = null
}
