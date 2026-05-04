package io.github.theestimator.repository

import io.github.theestimator.domain.AuditLog
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class AuditLogRepository : PanacheRepositoryBase<AuditLog, UUID>
