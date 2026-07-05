package io.github.theestimator.service

import io.github.theestimator.auth.DevAdminAuth
import io.github.theestimator.domain.Estimation
import io.github.theestimator.domain.Project
import io.github.theestimator.domain.User
import io.github.theestimator.domain.draft.DraftEstimationVersion
import io.github.theestimator.repository.DraftEstimationVersionRepository
import io.github.theestimator.repository.EstimationRepository
import io.github.theestimator.repository.ProjectRepository
import io.github.theestimator.repository.UserRepository
import io.github.theestimator.rest.dto.DraftUpdateDto
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@QuarkusTest
@ExtendWith(DevAdminAuth::class)
@Transactional
class UndoConflictIT {

    @Inject
    lateinit var undoService: UndoService

    @Inject
    lateinit var applier: DraftUpdateApplier

    @Inject
    lateinit var mapper: DraftVersionMapper

    @Inject
    lateinit var draftRepository: DraftEstimationVersionRepository

    @Inject
    lateinit var projectRepository: ProjectRepository

    @Inject
    lateinit var estimationRepository: EstimationRepository

    @Inject
    lateinit var userRepository: UserRepository

    private fun persistUser(subject: String): User =
        User().apply { entraSubjectId = subject; displayName = subject }.also { userRepository.persist(it) }

    // Apply a whole-draft snapshot and record it as `user`'s mutation.
    private fun record(draft: DraftEstimationVersion, next: DraftUpdateDto, user: User) {
        val beforeDto = draft.toUpdateDto()
        val before = mapper.toDomain(draft)
        applier.apply(draft, next)
        val afterDto = draft.toUpdateDto()
        val after = mapper.toDomain(draft)
        undoService.recordMutation(draft, before, after, beforeDto, afterDto, user)
    }

    @Test
    fun `A cannot undo once B has changed the draft on top`() {
        val userA = persistUser("conflict-A-" + UUID.randomUUID())
        val userB = persistUser("conflict-B-" + UUID.randomUUID())

        val project = Project().apply { name = "Conflict Project" }
        projectRepository.persist(project)
        val estimation = Estimation().apply { offer = "CONF-1"; this.project = project }
        estimationRepository.persist(estimation)
        val draft = DraftEstimationVersion().apply { versionNumber = 1; this.estimation = estimation }
        draftRepository.persist(draft)
        val eid = estimation.id!!

        record(draft, DraftUpdateDto(notes = "A's change"), userA)
        record(draft, DraftUpdateDto(notes = "B's change on top"), userB)

        val ex = assertThrows<UndoConflictException> { undoService.undoLastForUser(eid, userA) }
        assertEquals(userB.id, ex.blockingEntry.user?.id, "the blocker is B's entry")
    }
}
