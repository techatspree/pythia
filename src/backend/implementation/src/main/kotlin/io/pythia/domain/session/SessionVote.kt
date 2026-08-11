package io.pythia.domain.session

import io.pythia.domain.BaseEntity
import io.pythia.domain.EstimationBucket
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

// One estimator's three-point vote for one item in one phase. The submission
// time is BaseEntity.createdAt. Unique per (item, participant, phase) so a
// re-vote in the same phase overwrites rather than duplicates.
@Entity
@Table(name = "session_votes")
class SessionVote : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    var session: EstimationSession? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_item_id", nullable = false)
    var sessionItem: SessionItem? = null

    @Column(name = "participant_subject_id", nullable = false)
    var participantSubjectId: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false)
    var phase: SessionPhase = SessionPhase.PHASE1

    @Column(name = "min_effort", nullable = false)
    var minEffort: Double = 0.0

    @Column(name = "expected_effort", nullable = false)
    var expectedEffort: Double = 0.0

    @Column(name = "max_effort", nullable = false)
    var maxEffort: Double = 0.0

    // BUCKET_SAMPLED_PERT only (V17). Null for a PERT session's votes. Declared
    // on the entity as well as in the migration deliberately: %test/%dev build
    // the schema from Hibernate with Flyway off, so a column that lives only in
    // the migration would not exist where the tests run (the V15 lesson).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bucket_id")
    var bucket: EstimationBucket? = null

    // True when this vote also carries a three-point sample in the
    // min/expected/max_effort triple above, mirroring a BUCKETED node.
    @Column(name = "is_sample")
    var isSample: Boolean? = null
}
