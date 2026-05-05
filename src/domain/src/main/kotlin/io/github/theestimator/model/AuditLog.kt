package io.github.theestimator.model

import java.time.Instant
import java.util.UUID

@DomainEntity
data class AuditLog(
    val entityType: String,
    val entityId: UUID,
    val action: String,
    val userId: UUID? = null,
    val payload: String = "",
    val id: UUID? = null,
    val createdAt: Instant? = null
)
