package io.github.theestimator.domain.session

import io.github.theestimator.domain.BaseEntity
import io.github.theestimator.domain.Estimation
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import java.time.Instant

// A collaborative estimation session over a subset of an estimation's DRAFT
// leaf items (phase-11, task-063). The coarse lifecycle is `status`; per-item
// progress is tracked by SessionItem.status + currentItemIndex/currentPhase.
@Entity
@Table(name = "estimation_sessions")
class EstimationSession : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimation_id", nullable = false)
    var estimation: Estimation? = null

    @Column(name = "title", nullable = false)
    var title: String? = null

    @Column(name = "moderator_subject_id", nullable = false)
    var moderatorSubjectId: String? = null

    // Whether the moderator also estimates (votes). When false the moderator
    // only moderates: the GUI hides the estimate/revise forms and the service
    // rejects a vote from them (task-129).
    @Column(name = "moderator_estimates", nullable = false)
    var moderatorEstimates: Boolean = true

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: SessionStatus = SessionStatus.CREATED

    @Column(name = "current_item_index", nullable = false)
    var currentItemIndex: Int = 0

    @Enumerated(EnumType.STRING)
    @Column(name = "current_phase", nullable = false)
    var currentPhase: SessionPhase = SessionPhase.PHASE1

    @Column(name = "finalized_at")
    var finalizedAt: Instant? = null

    // Owned children — cascade so persisting the session persists them, and
    // delete cascades (matching the FK ON DELETE CASCADE). Votes are queried
    // via SessionVoteRepository rather than loaded eagerly with the session.
    @OneToMany(mappedBy = "session", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("position ASC")
    var items: MutableList<SessionItem> = mutableListOf()

    @OneToMany(mappedBy = "session", cascade = [CascadeType.ALL], orphanRemoval = true)
    var participants: MutableList<SessionParticipant> = mutableListOf()
}
