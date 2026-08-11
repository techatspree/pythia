package io.pythia.service

import io.pythia.domain.User
import io.pythia.domain.draft.DraftEstimationVersion
import io.pythia.domain.draft.DraftMutationLogEntry
import io.pythia.domain.draft.DraftMutationStatus
import io.pythia.model.EstimationVersion
import io.pythia.model.mutation.diff
import io.pythia.repository.DraftEstimationVersionRepository
import io.pythia.repository.DraftMutationLogRepository
import io.pythia.rest.dto.DraftUpdateDto
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import java.time.Instant
import java.util.UUID

// Records every draft mutation as an append-only log entry and replays the
// stored snapshots to undo/redo. The current user is passed in (resolved at the
// request edge, task-089), so these methods stay pure and testable.
@ApplicationScoped
class UndoService(
    private val draftRepository: DraftEstimationVersionRepository,
    private val logRepository: DraftMutationLogRepository,
    private val draftVersionMapper: DraftVersionMapper,
    private val jackson: DraftMutationJackson,
    private val applier: DraftUpdateApplier,
    private val auditLogService: AuditLogService
) {

    @Transactional
    fun recordMutation(
        draft: DraftEstimationVersion,
        before: EstimationVersion,
        after: EstimationVersion,
        beforeDto: DraftUpdateDto,
        afterDto: DraftUpdateDto,
        user: User
    ) {
        val mutation = before.diff(after) ?: return // no-op PUT — nothing changed

        val draftId = draft.id!!
        val revisionBefore = draft.revision
        val revisionAfter = draft.revision + 1
        val entry = DraftMutationLogEntry().apply {
            draftVersion = draft
            this.user = user
            sequenceNumber = logRepository.nextSequenceNumber(draftId)
            this.revisionBefore = revisionBefore
            this.revisionAfter = revisionAfter
            kind = mutation.kind
            payload = jackson.toJson(StoredMutation(mutation.kind, beforeDto, afterDto))
            inversePayload = jackson.toJson(StoredMutation(mutation.kind, afterDto, beforeDto))
            status = DraftMutationStatus.ACTIVE
        }
        logRepository.persist(entry)
        stamp(draft, user, revisionAfter)
        Log.info(
            "Recorded ${mutation.kind} on draft $draftId by ${user.id} " +
                "(revision $revisionBefore->$revisionAfter)"
        )
    }

    @Transactional
    fun undoLastForUser(estimationId: UUID, user: User): EstimationVersion {
        val draft = resolveDraft(estimationId)
        val draftId = draft.id!!
        val userLatest = logRepository.findLatestActiveForUserOnDraft(draftId, user.id!!)
            ?: throw WebApplicationException("Nothing to undo", Response.Status.NOT_FOUND)

        val draftLatest = logRepository.findLatestActiveForDraft(draftId)
        if (draftLatest != null && draftLatest.id != userLatest.id) {
            Log.info("Undo conflict on draft $draftId: blocked by newer entry ${draftLatest.id}")
            throw UndoConflictException(draftLatest)
        }

        val stored = jackson.fromJson(userLatest.inversePayload!!)
        applier.apply(draft, stored.after)
        userLatest.status = DraftMutationStatus.UNDONE
        userLatest.undoneAt = Instant.now()
        stamp(draft, user, draft.revision + 1)
        auditLogService.log(user.id, "DraftEstimationVersion", draftId, "UNDO", "revision=${draft.revision}")
        Log.info("Undid entry ${userLatest.id} on draft $draftId by ${user.id}")
        return draftVersionMapper.toDomain(draft)
    }

    @Transactional
    fun redoLastForUser(estimationId: UUID, user: User): EstimationVersion {
        val draft = resolveDraft(estimationId)
        val draftId = draft.id!!
        val undone = logRepository.findLatestUndoneForDraft(draftId)
            ?: throw WebApplicationException("Nothing to redo", Response.Status.NOT_FOUND)

        val activeLatest = logRepository.findLatestActiveForDraft(draftId)
        if (activeLatest != null && activeLatest.sequenceNumber > undone.sequenceNumber) {
            Log.info("Redo conflict on draft $draftId: superseded by active entry ${activeLatest.id}")
            throw UndoConflictException(activeLatest)
        }

        val stored = jackson.fromJson(undone.payload!!)
        applier.apply(draft, stored.after)
        undone.status = DraftMutationStatus.ACTIVE
        undone.undoneAt = null
        stamp(draft, user, draft.revision + 1)
        auditLogService.log(user.id, "DraftEstimationVersion", draftId, "REDO", "revision=${draft.revision}")
        Log.info("Redid entry ${undone.id} on draft $draftId by ${user.id}")
        return draftVersionMapper.toDomain(draft)
    }

    @Transactional
    fun historyFor(estimationId: UUID): List<DraftMutationLogEntry> =
        logRepository.findAllByDraftIdOrderBySequence(resolveDraft(estimationId).id!!)

    private fun resolveDraft(estimationId: UUID): DraftEstimationVersion =
        draftRepository.findByEstimationId(estimationId)
            ?: throw WebApplicationException("No draft found for estimation $estimationId", Response.Status.NOT_FOUND)

    private fun stamp(draft: DraftEstimationVersion, user: User, revision: Long) {
        draft.revision = revision
        draft.lastModifiedBy = user
        draft.lastModifiedAt = Instant.now()
    }
}
