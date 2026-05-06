package io.github.theestimator.domain.draft

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "draft_estimation_parameters")
class DraftEstimationParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Column(nullable = false)
    var name: String = ""

    @Column(name = "param_value", nullable = false)
    var value: Double = 0.0

    var comment: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    var version: DraftEstimationVersion? = null
}
