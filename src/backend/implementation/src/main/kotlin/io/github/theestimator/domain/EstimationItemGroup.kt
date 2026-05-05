package io.github.theestimator.domain

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import java.util.UUID

@Entity
@Table(name = "estimation_item_groups")
class EstimationItemGroup : BaseEntity() {

    @Column(name = "logical_id", nullable = false)
    var logicalId: UUID = UUID.randomUUID()

    @NotBlank
    @Column(nullable = false)
    var title: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id")
    var phase: ProjectPhase? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    var version: EstimationVersion? = null

    @OneToMany(mappedBy = "group", cascade = [CascadeType.ALL], orphanRemoval = true)
    var items: MutableList<EstimationItem> = mutableListOf()
}
