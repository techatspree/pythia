package io.github.theestimator.rest.dto

import io.github.theestimator.domain.session.ParticipantRole
import io.github.theestimator.domain.session.SessionItemStatus
import io.github.theestimator.domain.session.SessionPhase
import io.github.theestimator.domain.session.SessionStatus
import io.github.theestimator.model.VoteAggregate
import java.util.UUID

// Wire DTOs for the collaborative estimation session REST surface (task-064).
// Plain Jackson-serializable data classes. Votes + aggregate are only populated
// for an item once it is revealed (status PHASE2 or FINALIZED); during PHASE1
// the item exposes only submittedVoteCount (the blind phase).

data class TripleDto(
    val minEffort: Double,
    val expectedEffort: Double,
    val maxEffort: Double
)

data class SessionDto(
    val id: UUID,
    val title: String,
    val estimationId: UUID,
    val status: SessionStatus,
    val moderatorSubjectId: String,
    val currentItemIndex: Int,
    val currentPhase: SessionPhase,
    val moderatorEstimates: Boolean,
    val items: List<SessionItemDto>,
    val participants: List<ParticipantDto>
)

data class SessionItemDto(
    val nodeLogicalId: String,
    val description: String?,
    val position: Int,
    val status: SessionItemStatus,
    val discussionNotes: String?,
    val finalTriple: TripleDto?,
    val submittedVoteCount: Int,
    // Populated only when status == PHASE2 or FINALIZED (the reveal).
    val votes: List<VoteDto>?,
    val aggregate: AggregateDto?,
    // BUCKET_SAMPLED_PERT only: the bucket the group landed on, and every
    // assignment that lost the last-write-wins race. Null for PERT sessions and
    // before the reveal. The frontend can only see conflicts through this field
    // — the session SPI and its result types are domain-internal.
    val bucketAssignment: BucketAssignmentDto? = null
)

/** The LWW-resolved bucket for an item, with the writes that lost. */
data class BucketAssignmentDto(
    val bucketId: String,
    val source: String,
    val conflictingAssignments: List<AssignmentConflictDto>
)

/** One estimator's losing bucket write, surfaced so a disagreement stays visible. */
data class AssignmentConflictDto(
    val estimatorId: String,
    val displayName: String?,
    val bucketId: String,
    val at: String
)

data class VoteDto(
    val participantSubjectId: String,
    val displayName: String?,
    val triple: TripleDto,
    val phase: SessionPhase
)

data class AggregateDto(
    val meanMin: Double,
    val meanExpected: Double,
    val meanMax: Double,
    val pertMean: Double,
    val expectedMin: Double,
    val expectedMax: Double,
    val range: Double,
    val stdDev: Double,
    val coefficientOfVariation: Double,
    val diverged: Boolean,
    val voterCount: Int
)

data class ParticipantDto(
    val subjectId: String,
    val displayName: String?,
    val role: ParticipantRole,
    val agreed: Boolean
)

// Maps the domain aggregate (task-062) to the wire DTO. The domain names
// (expectedRange/expectedStdDev/expectedCv) are renamed for the API.
fun VoteAggregate.toDto(): AggregateDto = AggregateDto(
    meanMin = meanMin,
    meanExpected = meanExpected,
    meanMax = meanMax,
    pertMean = pertMean,
    expectedMin = expectedMin,
    expectedMax = expectedMax,
    range = expectedRange,
    stdDev = expectedStdDev,
    coefficientOfVariation = expectedCv,
    diverged = diverged,
    voterCount = voterCount
)

// Request bodies.
data class CreateSessionRequest(
    val estimationId: UUID,
    val title: String,
    val itemLogicalIds: List<String>,
    val moderatorEstimates: Boolean = true
)

data class NotesRequest(val notes: String)

data class VoteRequest(
    val minEffort: Double,
    val expectedEffort: Double,
    val maxEffort: Double
)

/**
 * Body of POST /api/sessions/{id}/votes/bucket — the BUCKET_SAMPLED_PERT vote.
 *
 * A separate payload rather than overloading [VoteRequest]: a bucket vote's
 * required field is the bucket, and its three-point triple is OPTIONAL (only a
 * sample carries one). Folding the two together would make every field nullable
 * and lose that distinction on the wire.
 */
data class BucketVoteRequest(
    val bucketId: UUID,
    val isSample: Boolean = false,
    val minEffort: Double = 0.0,
    val expectedEffort: Double = 0.0,
    val maxEffort: Double = 0.0
)

// Response of POST /api/sessions/{id}/ws-ticket — a short-lived single-use
// token the client puts on the WebSocket handshake query string (task-065).
data class WsTicketDto(val ticket: String)
