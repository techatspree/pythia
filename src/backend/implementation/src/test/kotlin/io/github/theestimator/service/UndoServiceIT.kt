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
import io.github.theestimator.rest.dto.EstimationNodeUpdateDto
import io.github.theestimator.rest.dto.EstimationParameterDto
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.ws.rs.WebApplicationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@QuarkusTest
@ExtendWith(DevAdminAuth::class)
@Transactional
class UndoServiceIT {

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

    private val v1 = DraftUpdateDto(
        notes = "v1",
        parameters = listOf(EstimationParameterDto(name = "dailyRate", value = 800.0)),
        roots = listOf(
            EstimationNodeUpdateDto(
                type = "GROUP", title = "G",
                children = listOf(
                    EstimationNodeUpdateDto(
                        type = "FIXED", description = "Item",
                        minEffort = 1.0, expectedEffort = 2.0, maxEffort = 3.0
                    )
                )
            )
        )
    )

    private fun persistUser(subject: String): User =
        User().apply { entraSubjectId = subject; displayName = subject }.also { userRepository.persist(it) }

    private fun persistDraft(): Pair<UUID, DraftEstimationVersion> {
        val project = Project().apply { name = "Undo Project" }
        projectRepository.persist(project)
        val estimation = Estimation().apply { offer = "UNDO-1"; this.project = project }
        estimationRepository.persist(estimation)
        val draft = DraftEstimationVersion().apply { versionNumber = 1; this.estimation = estimation }
        draftRepository.persist(draft)
        return estimation.id!! to draft
    }

    @Test
    fun `record then undo restores pre-update, redo restores post-update, exhausted undo is 404`() {
        val user = persistUser("undo-user-" + UUID.randomUUID())
        val (eid, draft) = persistDraft()

        val beforeDto = draft.toUpdateDto()
        val before = mapper.toDomain(draft)
        applier.apply(draft, v1)
        val afterDto = draft.toUpdateDto()
        val after = mapper.toDomain(draft)
        undoService.recordMutation(draft, before, after, beforeDto, afterDto, user)

        assertEquals(before, undoService.undoLastForUser(eid, user), "undo restores the pre-update state")
        assertEquals(after, undoService.redoLastForUser(eid, user), "redo restores the post-update state")
        assertEquals(before, undoService.undoLastForUser(eid, user), "undo again restores the pre-update state")

        assertThrows<WebApplicationException> { undoService.undoLastForUser(eid, user) }
    }
}
