package io.github.theestimator.domain.draft

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "draft_estimation_item_groups")
class DraftEstimationItemGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Column(name = "logical_id", nullable = false)
    var logicalId: UUID = UUID.randomUUID()

    @Column(nullable = false)
    var title: String = ""

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    var version: DraftEstimationVersion? = null

    @OneToMany(mappedBy = "group", cascade = [CascadeType.ALL], orphanRemoval = true)
    var items: MutableList<DraftEstimationItem> = mutableListOf()
}
