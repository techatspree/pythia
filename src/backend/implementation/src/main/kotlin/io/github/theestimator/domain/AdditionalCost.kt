package io.github.theestimator.domain

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank

@Entity
@Table(name = "additional_costs")
class AdditionalCost : BaseEntity() {

    @NotBlank
    @Column(nullable = false)
    var description: String? = null

    @Column(nullable = false)
    var amount: Double = 0.0

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: AdditionalCostType = AdditionalCostType.ONE_TIME

    @Column(name = "amount_per_week")
    var amountPerWeek: Double? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id")
    var phase: ProjectPhase? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    var version: EstimationVersion? = null
}
