package io.github.theestimator.domain.submitted

import io.github.theestimator.domain.Estimation
import jakarta.persistence.*
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

    @OneToMany(mappedBy = "version", cascade = [CascadeType.ALL], orphanRemoval = true)
    var parameters: MutableList<SubmittedEstimationParameter> = mutableListOf()

    @OneToMany(mappedBy = "version", cascade = [CascadeType.ALL], orphanRemoval = true)
    var effortDrivers: MutableList<SubmittedEffortDriver> = mutableListOf()

    @OneToMany(mappedBy = "version", cascade = [CascadeType.ALL], orphanRemoval = true)
    var phases: MutableList<SubmittedProjectPhase> = mutableListOf()

    @OneToMany(mappedBy = "version", cascade = [CascadeType.ALL], orphanRemoval = true)
    var itemGroups: MutableList<SubmittedEstimationItemGroup> = mutableListOf()

    @OneToMany(mappedBy = "version", cascade = [CascadeType.ALL], orphanRemoval = true)
    var additionalCosts: MutableList<SubmittedAdditionalCost> = mutableListOf()

    fun parameterValue(name: String): Double? =
        parameters.find { it.name == name }?.value
}
