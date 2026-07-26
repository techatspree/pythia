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
    val aggregate: AggregateDto?
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
    val itemLogicalIds: List<String>
)

data class NotesRequest(val notes: String)

data class VoteRequest(
    val minEffort: Double,
    val expectedEffort: Double,
    val maxEffort: Double
)
