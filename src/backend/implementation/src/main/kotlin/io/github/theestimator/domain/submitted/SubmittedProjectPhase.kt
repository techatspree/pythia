package io.github.theestimator.domain.submitted

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "submitted_project_phases")
class SubmittedProjectPhase {

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
    var version: SubmittedEstimationVersion? = null
}
