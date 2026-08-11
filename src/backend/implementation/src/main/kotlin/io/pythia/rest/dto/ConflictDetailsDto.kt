package io.pythia.rest.dto

import java.time.Instant
import java.util.UUID

// Body of a 409 from an undo/redo that would clobber a newer change.
data class ConflictDetailsDto(
    val message: String,
    val blockingSequenceNumber: Long,
    val blockingUserId: UUID,
    val blockingUserDisplayName: String,
    val blockingKind: String,
    val blockingCreatedAt: Instant,
    val currentDraftRevision: Long
)
