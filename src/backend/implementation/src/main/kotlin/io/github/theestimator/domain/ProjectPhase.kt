package io.github.theestimator.domain

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank

@Entity
@Table(name = "project_phases")
class ProjectPhase : BaseEntity() {

    @NotBlank
    @Column(nullable = false)
    var name: String? = null

    @NotBlank
    @Column(nullable = false)
    var abbreviation: String? = null

    @Column(name = "duration_weeks")
    var durationWeeks: Double? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    var version: EstimationVersion? = null
}
