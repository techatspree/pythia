package io.github.theestimator.domain.draft

import io.github.theestimator.domain.BaseEntity
import io.github.theestimator.domain.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "draft_mutation_log")
class DraftMutationLogEntry : BaseEntity() {

    // @OnDelete → Hibernate emits the FK with ON DELETE CASCADE (matching the
    // V8 migration), so deleting the draft (on submit/delete) removes its log
    // entries at the DB in BOTH the Flyway and Hibernate drop-and-create schemas.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draft_version_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var draftVersion: DraftEstimationVersion? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    @Column(name = "sequence_number", nullable = false)
    var sequenceNumber: Long = 0

    @Column(name = "revision_before", nullable = false)
    var revisionBefore: Long = 0

    @Column(name = "revision_after", nullable = false)
    var revisionAfter: Long = 0

    @Column(nullable = false, length = 64)
    var kind: String? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    var payload: String? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "inverse_payload", nullable = false)
    var inversePayload: String? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var status: DraftMutationStatus = DraftMutationStatus.ACTIVE

    @Column(name = "undone_at")
    var undoneAt: Instant? = null
}
