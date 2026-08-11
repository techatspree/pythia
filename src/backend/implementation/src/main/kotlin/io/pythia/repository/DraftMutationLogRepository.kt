package io.pythia.repository

import io.pythia.domain.draft.DraftMutationLogEntry
import io.pythia.domain.draft.DraftMutationStatus
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class DraftMutationLogRepository : PanacheRepositoryBase<DraftMutationLogEntry, UUID> {

    fun findActiveByDraftId(draftId: UUID): List<DraftMutationLogEntry> =
        list(
            "draftVersion.id = ?1 and status = ?2 order by sequenceNumber asc",
            draftId,
            DraftMutationStatus.ACTIVE
        )

    fun findAllByDraftIdOrderBySequence(draftId: UUID): List<DraftMutationLogEntry> =
        list("draftVersion.id = ?1 order by sequenceNumber asc", draftId)

    fun findLatestActiveForDraft(draftId: UUID): DraftMutationLogEntry? =
        find(
            "draftVersion.id = ?1 and status = ?2 order by sequenceNumber desc",
            draftId,
            DraftMutationStatus.ACTIVE
        ).firstResult()

    fun findLatestActiveForUserOnDraft(draftId: UUID, userId: UUID): DraftMutationLogEntry? =
        find(
            "draftVersion.id = ?1 and user.id = ?2 and status = ?3 order by sequenceNumber desc",
            draftId,
            userId,
            DraftMutationStatus.ACTIVE
        ).firstResult()

    fun findLatestUndoneForDraft(draftId: UUID): DraftMutationLogEntry? =
        find(
            "draftVersion.id = ?1 and status = ?2 order by sequenceNumber desc",
            draftId,
            DraftMutationStatus.UNDONE
        ).firstResult()

    fun nextSequenceNumber(draftId: UUID): Long {
        val max = getEntityManager()
            .createQuery(
                "select max(e.sequenceNumber) from DraftMutationLogEntry e where e.draftVersion.id = :draftId"
            )
            .setParameter("draftId", draftId)
            .singleResult as? Number
        return (max?.toLong() ?: 0L) + 1
    }
}
