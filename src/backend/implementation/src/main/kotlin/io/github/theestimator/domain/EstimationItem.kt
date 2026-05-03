package io.github.theestimator.domain

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank

@Entity
@Table(name = "estimation_items")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "item_type", discriminatorType = DiscriminatorType.STRING)
abstract class EstimationItem : BaseEntity() {

    @NotBlank
    @Column(nullable = false)
    var description: String? = null

    var code: String? = null

    @Column(name = "min_effort")
    var minEffort: Double? = null

    @Column(name = "expected_effort")
    var expectedEffort: Double? = null

    @Column(name = "max_effort")
    var maxEffort: Double? = null

    var assumptions: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id")
    var phase: ProjectPhase? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    var group: EstimationItemGroup? = null

    // Derived/calculated fields (persisted)
    var mean: Double? = null

    var variance: Double? = null

    @Column(name = "risk_surcharge")
    var riskSurcharge: Double? = null

    @Column(name = "driver_surcharge")
    var driverSurcharge: Double? = null

    @Column(name = "offer_pt")
    var offerPT: Double? = null

    var cost: Double? = null

    @Column(name = "offer_price")
    var offerPrice: Double? = null
}
