package io.pythia.domain.system

import io.pythia.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

// The organisation-wide effort-driver template (task-146). Copied into a draft
// that has NO submitted version to clone from, so a first estimate starts with
// the installation's standard drivers instead of an empty list.
//
// The unique constraint is declared here as well as in Flyway (V18) so the
// Hibernate-generated dev/test schema matches production (the V15 lesson).
@Entity
@Table(
    name = "system_effort_drivers",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_system_effort_drivers_position", columnNames = ["position"])
    ]
)
class SystemEffortDriver : BaseEntity() {

    @Column(name = "position", nullable = false)
    var position: Int = 0

    @Column(name = "description", nullable = false, length = 500)
    var description: String = ""

    @Column(name = "factor", nullable = false)
    var factor: Double = 0.0

    @Column(name = "comment", nullable = false, length = 2000)
    var comment: String = ""
}
