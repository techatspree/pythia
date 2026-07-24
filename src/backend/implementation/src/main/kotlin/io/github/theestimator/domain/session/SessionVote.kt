package io.github.theestimator.domain.session

import io.github.theestimator.domain.BaseEntity
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
}
