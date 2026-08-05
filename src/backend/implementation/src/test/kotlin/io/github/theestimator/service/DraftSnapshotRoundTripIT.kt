package io.github.theestimator.service

import io.github.theestimator.auth.DevAdminAuth
import io.github.theestimator.domain.AdditionalCostType
import io.github.theestimator.domain.Estimation
import io.github.theestimator.domain.Project
import io.github.theestimator.domain.draft.DraftAdditionalCost
import io.github.theestimator.domain.draft.DraftEstimationVersion
import io.github.theestimator.domain.draft.DraftFixedItemNode
import io.github.theestimator.domain.draft.DraftGroupNode
import io.github.theestimator.domain.draft.DraftProjectPhase
import io.github.theestimator.domain.draft.DraftTimeRelativeItemNode
import io.github.theestimator.repository.DraftEstimationVersionRepository
import io.github.theestimator.repository.EstimationRepository
import io.github.theestimator.repository.ProjectRepository
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@QuarkusTest
@ExtendWith(DevAdminAuth::class)
@Transactional
class DraftSnapshotRoundTripIT {

    @Inject
    lateinit var draftUpdateApplier: DraftUpdateApplier

    @Inject
    lateinit var draftVersionMapper: DraftVersionMapper

    @Inject
    lateinit var draftRepository: DraftEstimationVersionRepository

    @Inject
    lateinit var projectRepository: ProjectRepository

    @Inject
    lateinit var estimationRepository: EstimationRepository

    @Test
    fun `capture then restore is an identity on draft state`() {
        val project = Project().apply { name = "RoundTrip Project" }
        projectRepository.persist(project)
        val estimation = Estimation().apply {
            offer = "RT-OFFER"
            this.project = project
        }
        estimationRepository.persist(estimation)

        val draft = DraftEstimationVersion().apply {
            versionNumber = 1
            this.estimation = estimation
            notes = "round-trip notes"
        }

        val phase = DraftProjectPhase().apply {
            name = "Development"
            abbreviation = "DEV"
            durationWeeks = 4.0
            version = draft
        }
        draft.phases.add(phase)

        draft.dailyRate = 800.0

        val group = DraftGroupNode().apply {
            title = "Group"
            version = draft
            position = 0
            logicalId = UUID.randomUUID()
        }
        val fixed = DraftFixedItemNode().apply {
            version = draft
            parent = group
            position = 0
            logicalId = UUID.randomUUID()
            description = "Fixed item"
            minEffort = 1.0
            expectedEffort = 2.0
            maxEffort = 3.0
        }
        val innerGroup = DraftGroupNode().apply {
            title = "Inner"
            version = draft
            parent = group
            position = 1
            logicalId = UUID.randomUUID()
        }
        val timeRelative = DraftTimeRelativeItemNode().apply {
            version = draft
            parent = innerGroup
            position = 0
            logicalId = UUID.randomUUID()
            unit = "h/Woche"
            description = "Time-relative item"
            minEffort = 1.0
            expectedEffort = 2.0
            maxEffort = 3.0
            this.phase = phase
        }
        innerGroup.children.add(timeRelative)
        group.children.add(fixed)
        group.children.add(innerGroup)
        draft.roots.add(group)

        draft.additionalCosts.add(DraftAdditionalCost().apply {
            description = "Licenses"
            amount = 500.0
            type = AdditionalCostType.ONE_TIME
            this.phase = phase
            version = draft
        })

        draftRepository.persist(draft)

        val before = draftVersionMapper.toDomain(draft)
        val snapshot = draft.toUpdateDto()

        draftUpdateApplier.apply(draft, snapshot)

        assertEquals(before, draftVersionMapper.toDomain(draft))
        assertEquals(snapshot, draft.toUpdateDto())
    }
}
