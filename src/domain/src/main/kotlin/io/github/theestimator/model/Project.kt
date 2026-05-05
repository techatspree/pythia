package io.github.theestimator.model

@DomainEntity
class Project : BaseDomain() {
    var name: String? = null
    var description: String? = null
    var client: String? = null
    var status: ProjectStatus = ProjectStatus.ACTIVE
    var owner: User? = null
    var estimations: MutableList<Estimation> = mutableListOf()
}
