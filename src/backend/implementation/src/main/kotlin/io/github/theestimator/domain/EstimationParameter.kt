package io.github.theestimator.domain

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank

@Entity
@Table(name = "estimation_parameters")
class EstimationParameter : BaseEntity() {

    @NotBlank
    @Column(nullable = false)
    var name: String? = null

    @Column(nullable = false)
    var value: Double = 0.0

    var comment: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    var version: EstimationVersion? = null
}
