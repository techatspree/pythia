package io.pythia.service

import io.pythia.auth.DevAdminAuth
import io.pythia.domain.Estimation
import io.pythia.domain.Project
import io.pythia.domain.User
import io.pythia.domain.draft.DraftEstimationVersion
import io.pythia.repository.DraftEstimationVersionRepository
import io.pythia.repository.DraftMutationLogRepository
import io.pythia.repository.EstimationRepository
import io.pythia.repository.ProjectRepository
import io.pythia.repository.UserRepository
import io.pythia.rest.dto.DraftUpdateDto
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
