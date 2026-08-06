package io.github.theestimator.rest.dto

import java.time.Instant
import java.util.UUID

// One row of the draft's mutation history (both ACTIVE and UNDONE) — the
// `status` drives the GUI timeline rendering.
data class MutationLogEntryDto(
    val id: UUID,
    val sequenceNumber: Long,
    val revisionBefore: Long,
    val revisionAfter: Long,
    val userId: UUID,
    val userDisplayName: String,
    val kind: String,
    val status: String,
    val createdAt: Instant,
    val undoneAt: Instant?,
    // Structured, human-readable summary of what this mutation changed (task-110),
    // computed on read from the stored payload via EstimationVersion.diffSummary.
    val summary: List<ChangeSummaryDto>
)
