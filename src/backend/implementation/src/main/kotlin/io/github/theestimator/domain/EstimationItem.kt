package io.github.theestimator.domain

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank

@Entity
@Table(name = "estimation_items")
class EstimationItem : BaseEntity() {

    @NotBlank
    @Column(nullable = false)
    var description: String? = null

    var category: String? = null

    var optimistic: Double? = null

    var likely: Double? = null

    var pessimistic: Double? = null

    var assumptions: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    var version: EstimationVersion? = null
}
