package io.github.theestimator.model

@DomainEntity
class Estimation : BaseDomain() {
    var offer: String? = null
    var description: String? = null
    var project: Project? = null
    var currentVersion: EstimationVersion? = null
    var versions: MutableList<EstimationVersion> = mutableListOf()
}
