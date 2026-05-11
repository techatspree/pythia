package io.github.theestimator.domain.submitted

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "submitted_estimation_item_groups")
class SubmittedEstimationItemGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Column(name = "logical_id", nullable = false)
    var logicalId: UUID = UUID.randomUUID()

    @Column(nullable = false)
    var title: String = ""

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    var version: SubmittedEstimationVersion? = null

    @OneToMany(mappedBy = "group", cascade = [CascadeType.ALL], orphanRemoval = true)
    var items: MutableList<SubmittedEstimationItem> = mutableListOf()
}
