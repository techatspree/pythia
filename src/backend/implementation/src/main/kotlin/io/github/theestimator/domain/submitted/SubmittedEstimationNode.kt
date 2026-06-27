package io.github.theestimator.domain.submitted

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorType
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderColumn
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "submitted_estimation_nodes")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "node_type", discriminatorType = DiscriminatorType.STRING)
abstract class SubmittedEstimationNode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Column(name = "logical_id", nullable = false)
    var logicalId: UUID = UUID.randomUUID()

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    var version: SubmittedEstimationVersion? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: SubmittedEstimationNode? = null

    @Column(name = "position", nullable = false)
    var position: Int = 0

    var title: String? = null

    var description: String? = null

    var code: String? = null

    @Column(name = "min_effort")
    var minEffort: Double? = null

    @Column(name = "expected_effort")
    var expectedEffort: Double? = null

    @Column(name = "max_effort")
    var maxEffort: Double? = null

    var assumptions: String? = null

    var unit: String? = null

    @Column(name = "phase_abbreviation")
    var phaseAbbreviation: String? = null

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

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderColumn(name = "position")
    var children: MutableList<SubmittedEstimationNode> = mutableListOf()
}

@Entity
@DiscriminatorValue("GROUP")
class SubmittedGroupNode : SubmittedEstimationNode()

@Entity
@DiscriminatorValue("FIXED")
class SubmittedFixedItemNode : SubmittedEstimationNode()

@Entity
@DiscriminatorValue("TIME_RELATIVE")
class SubmittedTimeRelativeItemNode : SubmittedEstimationNode()
