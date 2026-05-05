package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
class Project(
    id: UUID? = null,
    createdAt: Instant? = null
) : BaseDomain(id, createdAt) {
    var name: String? = null
    var description: String? = null
    var client: String? = null
    var status: ProjectStatus = ProjectStatus.ACTIVE
    var owner: User? = null
    var estimations: MutableList<Estimation> = mutableListOf()
}
