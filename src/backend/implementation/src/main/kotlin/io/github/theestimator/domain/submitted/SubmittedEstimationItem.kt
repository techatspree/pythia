package io.github.theestimator.domain.submitted

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "submitted_estimation_items")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "item_type", discriminatorType = DiscriminatorType.STRING)
abstract class SubmittedEstimationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Column(name = "logical_id", nullable = false)
    var logicalId: UUID = UUID.randomUUID()

    @Column(nullable = false)
    var description: String = ""

    var code: String? = null

    @Column(name = "min_effort", nullable = false)
    var minEffort: Double = 0.0

    @Column(name = "expected_effort", nullable = false)
    var expectedEffort: Double = 0.0

    @Column(name = "max_effort", nullable = false)
    var maxEffort: Double = 0.0

    @Column(nullable = false)
    var mean: Double = 0.0

    @Column(nullable = false)
    var variance: Double = 0.0

    @Column(name = "risk_surcharge", nullable = false)
    var riskSurcharge: Double = 0.0

    @Column(name = "driver_surcharge", nullable = false)
    var driverSurcharge: Double = 0.0

    @Column(name = "offer_pt", nullable = false)
    var offerPT: Double = 0.0

    @Column(nullable = false)
    var cost: Double = 0.0

    @Column(name = "offer_price", nullable = false)
    var offerPrice: Double = 0.0

    var assumptions: String? = null

    @Column(name = "phase_abbreviation")
    var phaseAbbreviation: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    var group: SubmittedEstimationItemGroup? = null
}
