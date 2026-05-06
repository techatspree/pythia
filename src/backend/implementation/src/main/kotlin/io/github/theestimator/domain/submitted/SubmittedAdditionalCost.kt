package io.github.theestimator.domain.submitted

import io.github.theestimator.domain.AdditionalCostType
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "submitted_additional_costs")
class SubmittedAdditionalCost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Column(nullable = false)
    var description: String = ""

    @Column(nullable = false)
    var amount: Double = 0.0

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: AdditionalCostType = AdditionalCostType.ONE_TIME

    @Column(name = "amount_per_week")
    var amountPerWeek: Double? = null

    @Column(name = "phase_abbreviation")
    var phaseAbbreviation: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    var version: SubmittedEstimationVersion? = null
}
