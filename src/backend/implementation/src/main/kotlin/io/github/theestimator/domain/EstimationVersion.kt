package io.github.theestimator.domain

import jakarta.persistence.*
import jakarta.validation.constraints.NotNull

@Entity
@Table(
    name = "estimation_versions",
    uniqueConstraints = [UniqueConstraint(columnNames = ["estimation_id", "version_number"])]
)
class EstimationVersion : BaseEntity() {

    @NotNull
    @Column(name = "version_number", nullable = false)
    var versionNumber: Int? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: EstimationVersionStatus = EstimationVersionStatus.DRAFT

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null

    @Column(name = "total_effort")
    var totalEffort: Double? = null

    var notes: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimation_id", nullable = false)
    var estimation: Estimation? = null

    @OneToMany(mappedBy = "version", cascade = [CascadeType.ALL], orphanRemoval = true)
    var parameters: MutableList<EstimationParameter> = mutableListOf()

    @OneToMany(mappedBy = "version", cascade = [CascadeType.ALL], orphanRemoval = true)
    var effortDrivers: MutableList<EffortDriver> = mutableListOf()

    @OneToMany(mappedBy = "version", cascade = [CascadeType.ALL], orphanRemoval = true)
    var phases: MutableList<ProjectPhase> = mutableListOf()

    @OneToMany(mappedBy = "version", cascade = [CascadeType.ALL], orphanRemoval = true)
    var additionalCosts: MutableList<AdditionalCost> = mutableListOf()

    @OneToMany(mappedBy = "version", cascade = [CascadeType.ALL], orphanRemoval = true)
    var itemGroups: MutableList<EstimationItemGroup> = mutableListOf()
}
