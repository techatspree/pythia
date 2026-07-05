package io.github.theestimator.service

import io.github.theestimator.auth.DevAdminAuth
import io.github.theestimator.domain.Estimation
import io.github.theestimator.domain.Project
import io.github.theestimator.domain.User
import io.github.theestimator.domain.draft.DraftEstimationVersion
import io.github.theestimator.repository.DraftEstimationVersionRepository
import io.github.theestimator.repository.DraftMutationLogRepository
import io.github.theestimator.repository.EstimationRepository
import io.github.theestimator.repository.ProjectRepository
import io.github.theestimator.repository.UserRepository
import io.github.theestimator.rest.dto.DraftUpdateDto
import io.quarkus.narayana.jta.QuarkusTransaction
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

// Phases run in separate committed transactions (via QuarkusTransaction) so the
// ON DELETE CASCADE actually fires at commit and the assertion reads true DB
// state — a single test-transaction would see the pre-cascade session cache.
@QuarkusTest
@ExtendWith(DevAdminAuth::class)
class UndoServiceSubmitClearsLogIT {

    @Inject
    lateinit var undoService: UndoService

    @Inject
    lateinit var applier: DraftUpdateApplier

    @Inject
    lateinit var mapper: DraftVersionMapper

    @Inject
    lateinit var versionService: EstimationVersionService

    @Inject
    lateinit var logRepository: DraftMutationLogRepository

    @Inject
    lateinit var draftRepository: DraftEstimationVersionRepository

    @Inject
    lateinit var projectRepository: ProjectRepository

    @Inject
    lateinit var estimationRepository: EstimationRepository

    @Inject
    lateinit var userRepository: UserRepository

    data class Ids(val estimationId: UUID, val draftId: UUID)

    @Test
    fun `submitting the draft cascade-clears its mutation log`() {
        val ids = QuarkusTransaction.requiringNew().call<Ids> {
            val user = User().apply { entraSubjectId = "submit-" + UUID.randomUUID(); displayName = "S" }
                .also { userRepository.persist(it) }
            val project = Project().apply { name = "Submit Project" }.also { projectRepository.persist(it) }
            val estimation = Estimation().apply { offer = "SUB-1"; this.project = project }
                .also { estimationRepository.persist(it) }
            val draft = DraftEstimationVersion().apply { versionNumber = 1; this.estimation = estimation }
                .also { draftRepository.persist(it) }

            val beforeDto = draft.toUpdateDto()
            val before = mapper.toDomain(draft)
            applier.apply(draft, DraftUpdateDto(notes = "before submit"))
            undoService.recordMutation(draft, before, mapper.toDomain(draft), beforeDto, draft.toUpdateDto(), user)
            Ids(estimation.id!!, draft.id!!)
        }

        assertEquals(
            1,
            QuarkusTransaction.requiringNew().call { logRepository.findAllByDraftIdOrderBySequence(ids.draftId).size }
        )

        versionService.submitDraft(ids.estimationId)

        assertTrue(
            QuarkusTransaction.requiringNew().call { logRepository.findAllByDraftIdOrderBySequence(ids.draftId).isEmpty() },
            "the draft's mutation log must be gone after submit (ON DELETE CASCADE)"
        )
    }
}
