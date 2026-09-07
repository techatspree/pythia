package io.pythia.domain.draft

import io.pythia.model.PhaseDurationMode
import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "draft_project_phases")
class DraftProjectPhase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Column(nullable = false)
    var name: String = ""

    @Column(nullable = false)
    var abbreviation: String = ""

    @Column(name = "duration_weeks")
    var durationWeeks: Double? = null

    // Declared here as well as in V20 on purpose: %test/%dev build the schema
    // from Hibernate with Flyway off, so a migration-only column would not
    // exist where it is tested (the V15 lesson).
    @Column(name = "duration_mode", nullable = false)
    @Enumerated(EnumType.STRING)
    var durationMode: PhaseDurationMode = PhaseDurationMode.EXPLICIT

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    var version: DraftEstimationVersion? = null
}
