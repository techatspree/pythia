package io.github.theestimator.domain.draft

import io.github.theestimator.model.EstimationDefaults
import io.github.theestimator.domain.BaseEntity
import io.github.theestimator.domain.Estimation
import io.github.theestimator.domain.User
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.OrderColumn
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.time.Instant

@Entity
@Table(name = "draft_estimation_versions")
class DraftEstimationVersion : BaseEntity() {

    @Column(name = "version_number", nullable = false)
    var versionNumber: Int = 0

    var notes: String? = null

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimation_id", nullable = false, unique = true)
    var estimation: Estimation? = null

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
    var effortDrivers: MutableList<DraftEffortDriver> = mutableListOf()

    @OneToMany(mappedBy = "version", cascade = [CascadeType.ALL], orphanRemoval = true)
    var phases: MutableList<DraftProjectPhase> = mutableListOf()

    @OneToMany(mappedBy = "version", cascade = [CascadeType.ALL], orphanRemoval = true)
    @SQLRestriction("parent_id IS NULL")
    @OrderColumn(name = "position")
    var roots: MutableList<DraftEstimationNode> = mutableListOf()

    @OneToMany(mappedBy = "version", cascade = [CascadeType.ALL], orphanRemoval = true)
    var additionalCosts: MutableList<DraftAdditionalCost> = mutableListOf()

    @Column(nullable = false)
    var revision: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by")
    var lastModifiedBy: User? = null

    @Column(name = "last_modified_at")
    var lastModifiedAt: Instant? = null

}
