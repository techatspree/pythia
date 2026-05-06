package io.github.theestimator.domain.draft

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "draft_effort_drivers")
class DraftEffortDriver {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Column(nullable = false)
    var description: String = ""

    @Column(nullable = false)
    var factor: Double = 0.0

    var comment: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    var version: DraftEstimationVersion? = null
}
