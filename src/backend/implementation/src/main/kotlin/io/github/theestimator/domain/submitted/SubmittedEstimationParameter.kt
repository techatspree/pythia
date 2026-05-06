package io.github.theestimator.domain.submitted

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "submitted_estimation_parameters")
class SubmittedEstimationParameter {

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
    var version: SubmittedEstimationVersion? = null
}
