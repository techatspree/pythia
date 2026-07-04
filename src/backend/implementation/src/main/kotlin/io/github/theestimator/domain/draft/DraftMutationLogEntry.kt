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
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "draft_mutation_log")
class DraftMutationLogEntry : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draft_version_id", nullable = false)
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
