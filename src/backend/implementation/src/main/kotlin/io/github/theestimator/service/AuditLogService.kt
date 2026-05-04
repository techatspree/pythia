package io.github.theestimator.service

import io.github.theestimator.domain.AuditLog
import io.github.theestimator.repository.AuditLogRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.util.UUID

@ApplicationScoped
class AuditLogService(
    private val auditLogRepository: AuditLogRepository
) {

    @Transactional
    fun log(userId: UUID?, entityType: String, entityId: UUID?, action: String, payload: String? = null) {
        val entry = AuditLog().apply {
            this.userId = userId
            this.entityType = entityType
            this.entityId = entityId
            this.action = action
            this.payload = payload
        }
        auditLogRepository.persist(entry)
    }
}
