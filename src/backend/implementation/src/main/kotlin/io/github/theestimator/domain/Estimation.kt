package io.github.theestimator.domain

import io.github.theestimator.domain.draft.DraftEstimationVersion
import io.github.theestimator.domain.submitted.SubmittedEstimationVersion
import io.github.theestimator.method.EstimationMethod
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank

@Entity
@Table(name = "estimations")
class Estimation : BaseEntity() {

    @NotBlank
    @Column(nullable = false)
    var offer: String? = null

    var description: String? = null

    // Chosen at creation and immutable thereafter (no estimation-update
    // endpoint). VARCHAR(255) via @Enumerated(STRING); defaults to PERT.
    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false)
    var method: EstimationMethod = EstimationMethod.THREE_POINT_PERT

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    var project: Project? = null

    @OneToOne(mappedBy = "estimation", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var draftVersion: DraftEstimationVersion? = null

    @OneToMany(mappedBy = "estimation", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("versionNumber DESC")
    var submittedVersions: MutableList<SubmittedEstimationVersion> = mutableListOf()

    // Buckets of the bucket + sampled method (task-103); empty for PERT.
    // Shared across the draft and every submitted version of this estimation.
    @OneToMany(mappedBy = "estimation", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("position ASC")
    var buckets: MutableList<EstimationBucket> = mutableListOf()

    val latestSubmittedVersion: SubmittedEstimationVersion?
        get() = submittedVersions.maxByOrNull { it.versionNumber }
}
