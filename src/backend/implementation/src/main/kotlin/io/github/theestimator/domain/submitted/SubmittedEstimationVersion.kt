package io.github.theestimator.domain.submitted

import io.github.theestimator.model.EstimationDefaults
import io.github.theestimator.domain.Estimation
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderColumn
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.SQLRestriction
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "submitted_estimation_versions",
    uniqueConstraints = [UniqueConstraint(columnNames = ["estimation_id", "version_number"])]
)
class SubmittedEstimationVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Column(name = "version_number", nullable = false)
    var versionNumber: Int = 0

    @Column(name = "total_effort", nullable = false)
    var totalEffort: Double = 0.0

    var notes: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimation_id", nullable = false)
    var estimation: Estimation? = null

    @Column(name = "submitted_at", nullable = false)
    var submittedAt: Instant = Instant.now()

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()

    // Typed, non-renameable calculation inputs (task-138) — formerly generic
    // name/value rows that a GUI rename could make the lookup miss, silently
    // falling back to defaults.
    @Column(name = "daily_rate", nullable = false)
    var dailyRate: Double = EstimationDefaults.DAILY_RATE

    @Column(name = "std_dev_factor", nullable = false)
    var stdDevFactor: Double = EstimationDefaults.STD_DEV_FACTOR

    @Column(name = "sales_surcharge", nullable = false)
    var salesSurcharge: Double = EstimationDefaults.SALES_SURCHARGE

    @OneToMany(mappedBy = "version", cascade = [CascadeType.ALL], orphanRemoval = true)
    var effortDrivers: MutableList<SubmittedEffortDriver> = mutableListOf()

    @OneToMany(mappedBy = "version", cascade = [CascadeType.ALL], orphanRemoval = true)
    var phases: MutableList<SubmittedProjectPhase> = mutableListOf()

    @OneToMany(mappedBy = "version", cascade = [CascadeType.ALL], orphanRemoval = true)
    @SQLRestriction("parent_id IS NULL")
    @OrderColumn(name = "position")
    var roots: MutableList<SubmittedEstimationNode> = mutableListOf()

    @OneToMany(mappedBy = "version", cascade = [CascadeType.ALL], orphanRemoval = true)
    var additionalCosts: MutableList<SubmittedAdditionalCost> = mutableListOf()

}
