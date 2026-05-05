package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
data class AuditLog(
    val id: UUID? = null,
    val userId: UUID? = null,
    val entityType: String? = null,
    val entityId: UUID? = null,
    val action: String? = null,
    val payload: String? = null,
    val createdAt: Instant? = null
)
