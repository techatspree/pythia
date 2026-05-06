package io.github.theestimator.domain

import io.github.theestimator.domain.draft.DraftEstimationVersion
import io.github.theestimator.domain.submitted.SubmittedEstimationVersion
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
    var currentVersion: SubmittedEstimationVersion? = null

    @OneToOne(mappedBy = "estimation", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var draftVersion: DraftEstimationVersion? = null

    @OneToMany(mappedBy = "estimation", cascade = [CascadeType.ALL], orphanRemoval = true)
    var submittedVersions: MutableList<SubmittedEstimationVersion> = mutableListOf()
}
