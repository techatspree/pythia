package io.github.theestimator.domain.draft

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "draft_project_phases")
class DraftProjectPhase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Column(nullable = false)
    var name: String = ""

    @Column(nullable = false)
    var abbreviation: String = ""

    @Column(name = "duration_weeks")
    var durationWeeks: Double? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    var version: DraftEstimationVersion? = null
}
