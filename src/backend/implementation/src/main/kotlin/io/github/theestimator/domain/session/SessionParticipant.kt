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

// A user joined to a session (moderator or estimator). The join time is
// BaseEntity.createdAt. `agreed` is phase-2 agreement on the CURRENT item,
// reset when the item advances.
@Entity
@Table(name = "session_participants")
class SessionParticipant : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    var session: EstimationSession? = null

    @Column(name = "subject_id", nullable = false)
    var subjectId: String? = null

    @Column(name = "display_name", nullable = false)
    var displayName: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_role", nullable = false)
    var participantRole: ParticipantRole = ParticipantRole.ESTIMATOR

    @Column(name = "agreed", nullable = false)
    var agreed: Boolean = false
}
