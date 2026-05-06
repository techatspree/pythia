package io.github.theestimator.domain.draft

import io.github.theestimator.domain.BaseEntity
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "draft_estimation_items")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "item_type", discriminatorType = DiscriminatorType.STRING)
abstract class DraftEstimationItem : BaseEntity() {

    @Column(name = "logical_id", nullable = false)
    var logicalId: UUID = UUID.randomUUID()

    @Column(nullable = false)
    var description: String = ""

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
    var phase: DraftProjectPhase? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    var group: DraftEstimationItemGroup? = null
}
