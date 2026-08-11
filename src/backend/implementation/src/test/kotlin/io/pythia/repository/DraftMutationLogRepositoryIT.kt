package io.pythia.repository

import io.pythia.auth.DevAdminAuth
import io.pythia.domain.Estimation
import io.pythia.domain.Project
import io.pythia.domain.User
import io.pythia.domain.draft.DraftEstimationVersion
import io.pythia.domain.draft.DraftMutationLogEntry
import io.pythia.domain.draft.DraftMutationStatus
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@QuarkusTest
@ExtendWith(DevAdminAuth::class)
@Transactional
class DraftMutationLogRepositoryIT {

    @Inject
    lateinit var logRepository: DraftMutationLogRepository

    @Inject
    lateinit var draftVersionRepository: DraftEstimationVersionRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var projectRepository: ProjectRepository

    @Inject
    lateinit var estimationRepository: EstimationRepository

    private fun persistUser(): User {
        val user = User().apply { displayName = "Log User" }
        userRepository.persist(user)
        return user
    }

    private fun persistDraft(): DraftEstimationVersion {
        val project = Project().apply { name = "Log Project" }
        projectRepository.persist(project)
        val estimation = Estimation().apply {
            offer = "OFFER-LOG"
            this.project = project
        }
        estimationRepository.persist(estimation)
        val draft = DraftEstimationVersion().apply {
            versionNumber = 1
            this.estimation = estimation
        }
        draftVersionRepository.persist(draft)
        return draft
    }

    private fun entry(
        draft: DraftEstimationVersion,
        owner: User,
        seq: Long,
        entryStatus: DraftMutationStatus
    ) = DraftMutationLogEntry().apply {
        draftVersion = draft
        user = owner
        sequenceNumber = seq
        revisionBefore = seq - 1
        revisionAfter = seq
        kind = "REPLACE_WHOLE_DRAFT"
        payload = "{}"
        inversePayload = "{}"
        status = entryStatus
    }

    @Test
    fun `sequence numbering and active-versus-undone queries behave`() {
        val user = persistUser()
        val draft = persistDraft()
        val draftId = draft.id!!

        val e1 = entry(draft, user, 1, DraftMutationStatus.ACTIVE)
        val e2 = entry(draft, user, 2, DraftMutationStatus.ACTIVE)
        logRepository.persist(e1)
        logRepository.persist(e2)

        assertEquals(3L, logRepository.nextSequenceNumber(draftId))
        assertEquals(e2.id, logRepository.findLatestActiveForDraft(draftId)?.id)
        assertEquals(2, logRepository.findActiveByDraftId(draftId).size)

        e2.status = DraftMutationStatus.UNDONE
        logRepository.persist(e2)

        assertEquals(e1.id, logRepository.findLatestActiveForDraft(draftId)?.id)
        assertEquals(e2.id, logRepository.findLatestUndoneForDraft(draftId)?.id)
        assertNotNull(logRepository.findLatestActiveForUserOnDraft(draftId, user.id!!))
        assertEquals(e1.id, logRepository.findLatestActiveForUserOnDraft(draftId, user.id!!)?.id)
    }
}
