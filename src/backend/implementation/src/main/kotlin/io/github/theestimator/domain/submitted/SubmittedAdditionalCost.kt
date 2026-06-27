package io.github.theestimator.domain.submitted

import io.github.theestimator.domain.AdditionalCostType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
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
