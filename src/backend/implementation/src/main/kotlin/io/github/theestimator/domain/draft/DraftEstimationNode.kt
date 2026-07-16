package io.github.theestimator.domain.draft

import io.github.theestimator.domain.BaseEntity
import io.github.theestimator.domain.EstimationBucket
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorType
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderColumn
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "draft_estimation_nodes")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "node_type", discriminatorType = DiscriminatorType.STRING)
abstract class DraftEstimationNode : BaseEntity() {

    @Column(name = "logical_id", nullable = false)
    var logicalId: UUID = UUID.randomUUID()

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    var version: DraftEstimationVersion? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: DraftEstimationNode? = null

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id")
    var phase: DraftProjectPhase? = null

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderColumn(name = "position")
    var children: MutableList<DraftEstimationNode> = mutableListOf()
}

@Entity
@DiscriminatorValue("GROUP")
class DraftGroupNode : DraftEstimationNode()

@Entity
@DiscriminatorValue("FIXED")
class DraftFixedItemNode : DraftEstimationNode()

@Entity
@DiscriminatorValue("TIME_RELATIVE")
class DraftTimeRelativeItemNode : DraftEstimationNode()

// Bucket + sampled leaf (task-103). Reuses the inherited min/expected/max
// columns for a sample's optimistic/likely/pessimistic triple; non-samples
// leave them NULL and inherit the bucket average via calculate().
@Entity
@DiscriminatorValue("BUCKETED")
class DraftBucketedItemNode : DraftEstimationNode() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bucket_id")
    var bucket: EstimationBucket? = null

    @Column(name = "is_sample")
    var isSample: Boolean? = null
}
