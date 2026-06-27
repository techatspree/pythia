package io.github.theestimator.domain.draft

import io.github.theestimator.domain.BaseEntity
import io.github.theestimator.domain.Estimation
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.OrderColumn
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "draft_estimation_versions")
class DraftEstimationVersion : BaseEntity() {

    @Column(name = "version_number", nullable = false)
    var versionNumber: Int = 0

    var notes: String? = null

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimation_id", nullable = false, unique = true)
    var estimation: Estimation? = null

    @OneToMany(mappedBy = "version", cascade = [CascadeType.ALL], orphanRemoval = true)
    var parameters: MutableList<DraftEstimationParameter> = mutableListOf()

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

    fun parameterValue(name: String): Double? =
        parameters.find { it.name == name }?.value
}
