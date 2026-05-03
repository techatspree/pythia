package io.github.theestimator.domain

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank

@Entity
@Table(name = "effort_drivers")
class EffortDriver : BaseEntity() {

    @NotBlank
    @Column(nullable = false)
    var description: String? = null

    @Column(nullable = false)
    var factor: Double = 0.0

    var comment: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    var version: EstimationVersion? = null
}
