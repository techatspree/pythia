package io.github.theestimator.model


@DomainEntity
data class AuditLog(
    val entityType: String,
    val entityId: String,
    val action: String,
    val userId: String? = null,
    val payload: String = "",
    val id: String? = null,
    val createdAt: String? = null
)
