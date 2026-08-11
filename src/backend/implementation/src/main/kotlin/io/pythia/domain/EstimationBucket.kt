package io.pythia.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

// A bucket of the bucket + sampled method (task-103). Owned by the Estimation
// (shared across its draft + submitted versions); bucketed leaf nodes reference
// it, ordered within an estimation by `position`.
//
// The id is CLIENT-ASSIGNED (like a node's logicalId), not @GeneratedValue: a
// single draft PUT sends new buckets together with the leaves that reference
// them by id, so the id must be known client-side before persistence.
// The unique constraint is declared here as well as in Flyway (V10/V15) so the
// Hibernate-generated dev/test schema MATCHES production. Without it those
// schemas had no constraint at all, so a bucket-reorder bug that reliably broke
// the Flyway-managed cluster could not be reproduced — or regression-tested —
// locally.
@Entity
@Table(
    name = "estimation_buckets",
    uniqueConstraints = [
        jakarta.persistence.UniqueConstraint(
            name = "uq_estimation_buckets_position",
            columnNames = ["estimation_id", "position"]
        )
    ]
)
class EstimationBucket {

    @Id
    var id: UUID? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimation_id", nullable = false)
    var estimation: Estimation? = null

    @Column(name = "position", nullable = false)
    var position: Int = 0

    @Column(name = "label", nullable = false)
    var label: String = ""

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null

    @Column(name = "updated_at")
    var updatedAt: Instant? = null

    @PrePersist
    fun onPrePersist() {
        if (id == null) id = UUID.randomUUID()
        createdAt = Instant.now()
        updatedAt = Instant.now()
    }

    @PreUpdate
    fun onPreUpdate() {
        updatedAt = Instant.now()
    }
}
