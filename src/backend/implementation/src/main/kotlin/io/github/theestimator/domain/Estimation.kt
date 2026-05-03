package io.github.theestimator.domain

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank

@Entity
@Table(name = "estimations")
class Estimation : BaseEntity() {

    @NotBlank
    @Column(nullable = false)
    var offer: String? = null

    var description: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    var project: Project? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_version_id")
    var currentVersion: EstimationVersion? = null

    @OneToMany(mappedBy = "estimation", cascade = [CascadeType.ALL], orphanRemoval = true)
    var versions: MutableList<EstimationVersion> = mutableListOf()
}
