package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
class AuditLog {
    var id: UUID? = null
    var userId: UUID? = null
    var entityType: String? = null
    var entityId: UUID? = null
    var action: String? = null
    var payload: String? = null
    var createdAt: Instant? = null
}
