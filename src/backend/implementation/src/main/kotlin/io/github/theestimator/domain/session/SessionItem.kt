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

// One work item under estimation in a session, walked through the two-phase
// Delphi flow. `nodeLogicalId` is a SOFT reference to the draft leaf's
// logical_id (not an FK). The final_*_effort triple is written back on finalize
// (THREE_POINT_PERT shape).
@Entity
@Table(name = "session_items")
class SessionItem : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    var session: EstimationSession? = null

    @Column(name = "node_logical_id", nullable = false)
    var nodeLogicalId: String? = null

    @Column(name = "position", nullable = false)
    var position: Int = 0

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: SessionItemStatus = SessionItemStatus.PENDING

    @Column(name = "discussion_notes")
    var discussionNotes: String? = null

    @Column(name = "final_min_effort")
    var finalMinEffort: Double? = null

    @Column(name = "final_expected_effort")
    var finalExpectedEffort: Double? = null

    @Column(name = "final_max_effort")
    var finalMaxEffort: Double? = null
}
