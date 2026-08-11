package io.pythia.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "audit_logs")
class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Column(name = "user_id")
    var userId: UUID? = null

    @Column(name = "entity_type")
    var entityType: String? = null

    @Column(name = "entity_id")
    var entityId: UUID? = null

    var action: String? = null

    @Column(columnDefinition = "TEXT")
    var payload: String? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null

    @PrePersist
    fun onPrePersist() {
        createdAt = Instant.now()
    }
}
