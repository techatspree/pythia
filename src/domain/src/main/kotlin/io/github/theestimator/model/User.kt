package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
class User(
    id: UUID? = null,
    createdAt: Instant? = null
) : BaseDomain(id, createdAt) {
    var entraSubjectId: String? = null
    var displayName: String? = null
}
