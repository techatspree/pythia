package io.github.theestimator.rest.dto

import io.github.theestimator.domain.draft.DraftMutationLogEntry

// Entry → DTO. Reads the LAZY `user` association, so callers must invoke this
// within an active transaction (the history endpoint is @Transactional).
fun DraftMutationLogEntry.toLogDto() = MutationLogEntryDto(
    id = id!!,
    sequenceNumber = sequenceNumber,
    revisionBefore = revisionBefore,
    revisionAfter = revisionAfter,
    userId = user!!.id!!,
    userDisplayName = user!!.displayName ?: "",
    kind = kind ?: "",
    status = status.name,
    createdAt = createdAt!!,
    undoneAt = undoneAt
)
