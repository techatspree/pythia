package io.github.theestimator.domain.draft

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "draft_additional_costs")
class DraftAdditionalCost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Column(nullable = false)
    var description: String = ""

    @Column(nullable = false)
    var amount: Double = 0.0

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: io.github.theestimator.domain.AdditionalCostType = io.github.theestimator.domain.AdditionalCostType.ONE_TIME

    @Column(name = "amount_per_week")
    var amountPerWeek: Double? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id")
    var phase: DraftProjectPhase? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    var version: DraftEstimationVersion? = null
}
