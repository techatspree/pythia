package io.github.theestimator.model

@DomainEntity
class EstimationItemGroup : BaseDomain() {
    var title: String? = null
    var phase: ProjectPhase? = null
    var version: EstimationVersion? = null
    var items: MutableList<EstimationItem> = mutableListOf()
}
