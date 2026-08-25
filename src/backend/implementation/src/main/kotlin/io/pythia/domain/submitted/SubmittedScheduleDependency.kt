package io.pythia.domain.submitted

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

/**
 * One finish-to-start edge between two root nodes of a submitted snapshot, by
 * `logicalId`.
 *
 * The UNIQUE constraint is declared HERE as well as in `V19` on purpose:
 * `%test`/`%dev` build the schema from Hibernate with Flyway off, so a
 * migration-only constraint would not exist where it is tested (the V15
 * lesson).
 */
@Entity
@Table(
    name = "submitted_schedule_dependencies",
    uniqueConstraints = [UniqueConstraint(
        name = "uq_submitted_schedule_dep",
        columnNames = ["version_id", "from_logical_id", "to_logical_id"]
    )]
)
class SubmittedScheduleDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Column(name = "from_logical_id", nullable = false)
    var fromLogicalId: String = ""

    @Column(name = "to_logical_id", nullable = false)
    var toLogicalId: String = ""

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    var version: SubmittedEstimationVersion? = null
}
