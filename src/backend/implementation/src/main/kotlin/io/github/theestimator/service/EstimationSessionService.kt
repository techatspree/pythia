package io.github.theestimator.service

import io.github.theestimator.domain.User
import io.github.theestimator.domain.draft.DraftEstimationVersion
import io.github.theestimator.domain.draft.DraftGroupNode
import io.github.theestimator.domain.session.EstimationSession
import io.github.theestimator.domain.session.ParticipantRole
import io.github.theestimator.domain.session.SessionItem
import io.github.theestimator.domain.session.SessionItemStatus
import io.github.theestimator.domain.session.SessionParticipant
import io.github.theestimator.domain.session.SessionPhase
import io.github.theestimator.domain.session.SessionStatus
import io.github.theestimator.domain.session.SessionVote
import io.github.theestimator.method.EstimationMethod
import io.github.theestimator.method.EstimationMethodRegistry
import io.github.theestimator.method.EstimationMethodSessionSupport
import io.github.theestimator.method.bucketsampled.AssignmentResult
import io.github.theestimator.method.bucketsampled.BucketSampledSessionResult
import io.github.theestimator.method.bucketsampled.BucketVoteInput
import io.github.theestimator.method.bucketsampled.EstimatorBucketAssignment
import io.github.theestimator.method.bucketsampled.SampleTriple
import io.github.theestimator.method.threepoint.PertReduction
import io.github.theestimator.method.threepoint.PertVoteInput
import io.github.theestimator.model.EstimatorVote
import io.github.theestimator.model.VoteAggregate
import kotlinx.datetime.Instant as KotlinInstant
import io.github.theestimator.repository.DraftEstimationVersionRepository
import io.github.theestimator.repository.EstimationRepository
import io.github.theestimator.repository.EstimationSessionRepository
import io.github.theestimator.repository.SessionVoteRepository
import io.github.theestimator.rest.dto.AggregateDto
import io.github.theestimator.rest.dto.AssignmentConflictDto
import io.github.theestimator.rest.dto.BucketAssignmentDto
import io.github.theestimator.rest.dto.EstimationNodeUpdateDto
import io.github.theestimator.rest.dto.ParticipantDto
import io.github.theestimator.rest.dto.SessionDto
import io.github.theestimator.rest.dto.SessionItemDto
import io.github.theestimator.rest.dto.TripleDto
import io.github.theestimator.rest.dto.VoteDto
import io.github.theestimator.rest.dto.toDto
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import java.time.Instant
import java.util.UUID

// HTTP control plane for collaborative estimation sessions (task-064). Drives
// the two-phase Delphi lifecycle on top of the task-063 persistence. Aggregation
// is NEVER computed here — it is delegated to the estimation method's
// EstimationMethodSessionSupport, resolved from the domain
// EstimationMethodRegistry by the session's estimation method.
// Every mutation fires SessionEventPublisher so task-065 can push.
// Building the DTO happens inside the transaction (lazy graph is loaded here).
// The lifecycle is one cohesive service (many small methods) with DI-injected
// collaborators and guard-clause validation, so TooManyFunctions /
// LongParameterList / ThrowsCount are expected here.
@Suppress("TooManyFunctions", "LongParameterList", "ThrowsCount")
@ApplicationScoped
class EstimationSessionService(
    private val sessionRepository: EstimationSessionRepository,
    private val voteRepository: SessionVoteRepository,
    private val estimationRepository: EstimationRepository,
    private val draftRepository: DraftEstimationVersionRepository,
    private val draftVersionMapper: DraftVersionMapper,
    private val draftUpdateApplier: DraftUpdateApplier,
    private val undoService: UndoService,
    private val eventPublisher: SessionEventPublisher
) {

    @Transactional
    fun createSession(
        estimationId: UUID,
        title: String,
        itemLogicalIds: List<String>,
        moderatorSubjectId: String,
        moderatorDisplayName: String?,
        moderatorEstimates: Boolean
    ): SessionDto {
        Log.info(
            "Creating session over estimation $estimationId (${itemLogicalIds.size} items) " +
                "by $moderatorSubjectId (moderatorEstimates=$moderatorEstimates)"
        )
        val estimation = estimationRepository.findById(estimationId)
            ?: throw notFound("Estimation not found: $estimationId")
        val draft = draftRepository.findByEstimationId(estimationId)
            ?: throw badRequest("Estimation $estimationId has no draft to run a session over")

        val leafIds = leafLogicalIds(draft)
        val unknown = itemLogicalIds.filterNot { it in leafIds }
        if (unknown.isNotEmpty()) throw badRequest("Not draft leaves: $unknown")

        val session = EstimationSession().apply {
            this.estimation = estimation
            this.title = title
            this.moderatorSubjectId = moderatorSubjectId
            this.status = SessionStatus.CREATED
            this.currentItemIndex = 0
            this.currentPhase = SessionPhase.PHASE1
            this.moderatorEstimates = moderatorEstimates
        }
        itemLogicalIds.forEachIndexed { idx, lid ->
            session.items.add(SessionItem().apply {
                this.session = session
                this.nodeLogicalId = lid
                this.position = idx
                this.status = SessionItemStatus.PENDING
            })
        }
        session.participants.add(SessionParticipant().apply {
            this.session = session
            this.subjectId = moderatorSubjectId
            this.displayName = moderatorDisplayName ?: moderatorSubjectId
            this.participantRole = ParticipantRole.MODERATOR
        })
        sessionRepository.persist(session)
        Log.info("Created session ${session.id} over estimation $estimationId")
        eventPublisher.published(session.id.toString())
        return buildDto(session)
    }

    @Transactional
    fun getSession(id: UUID): SessionDto = buildDto(requireSession(id))

    // Throws 404 if the session is missing, 403 if the subject is not a joined
    // participant. Used to gate WebSocket ticket issuance (task-065).
    @Transactional
    fun assertParticipant(id: UUID, subjectId: String) {
        val session = requireSession(id)
        if (session.participants.none { it.subjectId == subjectId }) {
            throw WebApplicationException("$subjectId is not a participant of session $id", Response.Status.FORBIDDEN)
        }
    }

    @Transactional
    fun isParticipant(id: UUID, subjectId: String): Boolean {
        val session = sessionRepository.findById(id) ?: return false
        return session.participants.any { it.subjectId == subjectId }
    }

    @Transactional
    fun listSessions(estimationId: UUID): List<SessionDto> =
        sessionRepository.findByEstimationId(estimationId).map { buildDto(it) }

    // Joinable (CREATED/RUNNING/SUSPENDED) sessions across every estimation —
    // powers the frontend's open-sessions join list when no estimationId is
    // supplied, and is how a parked session is found again and resumed.
    @Transactional
    fun listJoinableSessions(): List<SessionDto> {
        val sessions = sessionRepository.findJoinable()
        Log.info("Listing ${sessions.size} joinable session(s)")
        return sessions.map { buildDto(it) }
    }

    @Transactional
    fun join(id: UUID, subjectId: String, displayName: String?): SessionDto {
        val session = requireSession(id)
        if (session.participants.none { it.subjectId == subjectId }) {
            session.participants.add(SessionParticipant().apply {
                this.session = session
                this.subjectId = subjectId
                this.displayName = displayName ?: subjectId
                this.participantRole = ParticipantRole.ESTIMATOR
            })
            Log.info("$subjectId joined session $id")
            eventPublisher.published(id.toString())
        }
        return buildDto(session)
    }

    @Transactional
    fun start(id: UUID, moderatorSubjectId: String): SessionDto {
        val session = requireModerator(id, moderatorSubjectId)
        if (session.status != SessionStatus.CREATED) throw conflict("Session $id is not in CREATED")
        session.status = SessionStatus.RUNNING
        session.currentItemIndex = 0
        session.currentPhase = SessionPhase.PHASE1
        currentItem(session)?.let { it.status = SessionItemStatus.PHASE1 }
        Log.info(
            "Started session $id (estimation ${session.estimation?.id}, " +
                "method ${session.estimation?.method})"
        )
        eventPublisher.published(id.toString())
        return buildDto(session)
    }

    @Transactional
    fun updateCurrentNotes(id: UUID, moderatorSubjectId: String, notes: String): SessionDto {
        val session = requireModerator(id, moderatorSubjectId)
        val item = currentItem(session) ?: throw conflict("Session $id has no current item")
        item.discussionNotes = notes
        eventPublisher.published(id.toString())
        return buildDto(session)
    }

    @Transactional
    fun revealPhase2(id: UUID, moderatorSubjectId: String): SessionDto {
        val session = requireModerator(id, moderatorSubjectId)
        requireRunning(session, id)
        val item = currentItem(session) ?: throw conflict("Session $id has no current item")
        if (session.currentPhase != SessionPhase.PHASE1 || item.status != SessionItemStatus.PHASE1) {
            throw conflict("Current item of session $id is not in PHASE1")
        }
        session.currentPhase = SessionPhase.PHASE2
        item.status = SessionItemStatus.PHASE2
        Log.info("Revealed PHASE2 for item ${item.position} of session $id")
        eventPublisher.published(id.toString())
        return buildDto(session)
    }

    /**
     * Shared guard for every vote shape: the session must be running, the caller
     * must be a participant, and a moderate-only moderator may not vote. Returns
     * the session + the item being voted on.
     */
    private fun requireVotable(id: UUID, subjectId: String): Pair<EstimationSession, SessionItem> {
        val session = requireSession(id)
        requireRunning(session, id)
        if (session.participants.none { it.subjectId == subjectId }) {
            throw conflict("$subjectId is not a participant of session $id")
        }
        if (subjectId == session.moderatorSubjectId && !session.moderatorEstimates) {
            Log.warn("Rejected vote from moderate-only moderator $subjectId on session $id")
            throw conflict("The moderator does not estimate in session $id")
        }
        val item = currentItem(session) ?: throw conflict("Session $id has no current item")
        return session to item
    }

    @Transactional
    fun submitVote(id: UUID, subjectId: String, min: Double, expected: Double, max: Double): SessionDto {
        val (session, item) = requireVotable(id, subjectId)
        val phase = session.currentPhase
        val existing = voteRepository.findByItemAndPhase(item.id!!, phase)
            .firstOrNull { it.participantSubjectId == subjectId }
        if (existing != null) {
            existing.minEffort = min
            existing.expectedEffort = expected
            existing.maxEffort = max
        } else {
            voteRepository.persist(SessionVote().apply {
                this.session = session
                this.sessionItem = item
                this.participantSubjectId = subjectId
                this.phase = phase
                this.minEffort = min
                this.expectedEffort = expected
                this.maxEffort = max
            })
        }
        Log.debug("Vote by $subjectId on item ${item.position} ($phase) of session $id")
        eventPublisher.published(id.toString())
        return buildDto(session)
    }

    /**
     * BUCKET_SAMPLED_PERT vote: a bucket assignment, plus an optional three-point
     * sample reusing the same min/expected/max columns a BUCKETED node uses.
     */
    @Transactional
    fun submitBucketVote(
        id: UUID,
        subjectId: String,
        bucketId: UUID,
        isSample: Boolean,
        min: Double,
        expected: Double,
        max: Double
    ): SessionDto {
        val (session, item) = requireVotable(id, subjectId)
        if (session.estimation?.method != EstimationMethod.BUCKET_SAMPLED_PERT) {
            Log.warn("Rejected bucket vote on session $id: estimation method is ${session.estimation?.method}")
            throw conflict("Session $id does not use the bucket+sampled method")
        }
        // The bucket must belong to THIS estimation — otherwise a caller could
        // file a vote against another offer's bucket.
        val bucket = session.estimation?.buckets?.firstOrNull { it.id == bucketId }
            ?: run {
                Log.warn("Rejected bucket vote on session $id: bucket $bucketId is not one of the estimation's")
                throw conflict("Bucket $bucketId does not belong to session $id's estimation")
            }
        val phase = session.currentPhase
        val existing = voteRepository.findByItemAndPhase(item.id!!, phase)
            .firstOrNull { it.participantSubjectId == subjectId }
        if (existing != null) {
            existing.bucket = bucket
            existing.isSample = isSample
            existing.minEffort = min
            existing.expectedEffort = expected
            existing.maxEffort = max
        } else {
            voteRepository.persist(SessionVote().apply {
                this.session = session
                this.sessionItem = item
                this.participantSubjectId = subjectId
                this.phase = phase
                this.bucket = bucket
                this.isSample = isSample
                this.minEffort = min
                this.expectedEffort = expected
                this.maxEffort = max
            })
        }
        Log.debug("Bucket vote by $subjectId on item ${item.position} ($phase) of session $id -> bucket $bucketId")
        eventPublisher.published(id.toString())
        return buildDto(session)
    }

    @Transactional
    fun agree(id: UUID, subjectId: String): SessionDto {
        val session = requireSession(id)
        val participant = session.participants.firstOrNull { it.subjectId == subjectId }
            ?: throw conflict("$subjectId is not a participant of session $id")
        participant.agreed = true
        eventPublisher.published(id.toString())
        return buildDto(session)
    }

    @Transactional
    fun cancel(id: UUID, moderatorSubjectId: String): SessionDto {
        val session = requireModerator(id, moderatorSubjectId)
        session.status = SessionStatus.CANCELLED
        Log.info("Cancelled session $id")
        eventPublisher.published(id.toString())
        return buildDto(session)
    }

    /**
     * Park a running session. Nothing else is touched — item statuses, votes,
     * notes, currentItemIndex and currentPhase all stay as they are, so a resume
     * continues exactly where the room stopped.
     */
    @Transactional
    fun suspend(id: UUID, moderatorSubjectId: String): SessionDto {
        val session = requireModerator(id, moderatorSubjectId)
        requireRunning(session, id)
        session.status = SessionStatus.SUSPENDED
        Log.info("Suspended session $id (now ${session.status})")
        eventPublisher.published(id.toString())
        return buildDto(session)
    }

    /** Un-park a suspended session, back to exactly the item and phase it held. */
    @Transactional
    fun resume(id: UUID, moderatorSubjectId: String): SessionDto {
        val session = requireModerator(id, moderatorSubjectId)
        if (session.status != SessionStatus.SUSPENDED) throw conflict("Session $id is not SUSPENDED")
        session.status = SessionStatus.RUNNING
        Log.info("Resumed session $id (now ${session.status})")
        eventPublisher.published(id.toString())
        return buildDto(session)
    }

    /**
     * Close a session before its last item, KEEPING the results gathered so far.
     * Every already-finalized item was written back to the draft leaf as it was
     * finalized, so there is nothing to flush here. The in-flight item's votes are
     * deliberately NOT aggregated — finalizing a partially-voted item would invent
     * a number nobody agreed on; it simply keeps no final estimate.
     */
    @Transactional
    fun endEarly(id: UUID, moderatorSubjectId: String): SessionDto {
        val session = requireModerator(id, moderatorSubjectId)
        if (session.status != SessionStatus.RUNNING && session.status != SessionStatus.SUSPENDED) {
            throw conflict("Session $id is not RUNNING or SUSPENDED")
        }
        session.status = SessionStatus.ENDED_EARLY
        session.finalizedAt = Instant.now()
        Log.info("Ended session $id early (now ${session.status})")
        eventPublisher.published(id.toString())
        return buildDto(session)
    }

    @Transactional
    fun finalizeCurrent(id: UUID, moderator: User): SessionDto {
        val session = requireModerator(id, moderator.entraSubjectId ?: "")
        requireRunning(session, id)
        val item = currentItem(session) ?: throw conflict("Session $id has no current item")
        if (session.currentPhase != SessionPhase.PHASE2 || item.status != SessionItemStatus.PHASE2) {
            throw conflict("Current item of session $id is not in PHASE2")
        }

        val votes = effectiveVotes(item)
        if (votes.isEmpty()) throw conflict("No votes to finalize item ${item.position} of session $id")

        val support = sessionSupport(session)
        // Each method contributes its own final values. PERT finalizes the mean
        // triple; bucket+sampled finalizes the LWW bucket plus the averaged
        // sample (a non-sample item legitimately finalizes 0/0/0 — its effort is
        // derived from its bucket's samples by EstimationVersion.calculate(),
        // not stored on the leaf).
        val bucketId: UUID?
        if (support.method == EstimationMethod.BUCKET_SAMPLED_PERT) {
            val reduction = reduceBucket(support, votes)
            bucketId = reduction.assignment?.bucketId?.let(UUID::fromString)
            val sample = reduction.averagedSample
            item.finalMinEffort = sample?.optimistic ?: 0.0
            item.finalExpectedEffort = sample?.likely ?: 0.0
            item.finalMaxEffort = sample?.pessimistic ?: 0.0
            Log.info(
                "Reduced item ${item.position} of session $id to bucket ${reduction.assignment?.bucketId} " +
                    "(source=${reduction.assignment?.source}, " +
                    "conflicts=${reduction.assignment?.conflictingAssignments?.size ?: 0}, " +
                    "sampled=${sample != null})"
            )
        } else {
            bucketId = null
            val aggregate = reducePert(support, votes)
            item.finalMinEffort = aggregate.meanMin
            item.finalExpectedEffort = aggregate.meanExpected
            item.finalMaxEffort = aggregate.meanMax
        }
        item.status = SessionItemStatus.FINALIZED

        writeBackToDraft(session, item, moderator, bucketId)
        session.participants.forEach { it.agreed = false }
        advance(session)

        Log.info(
            "Finalized item ${item.position} of session $id " +
                "(${item.finalMinEffort}/${item.finalExpectedEffort}/${item.finalMaxEffort}" +
                (bucketId?.let { ", bucket=$it" } ?: "") + "); " +
                "session now ${session.status} at index ${session.currentItemIndex}"
        )
        eventPublisher.published(id.toString())
        return buildDto(session)
    }

    // ── internal helpers ──────────────────────────────────────────────────────

    private fun advance(session: EstimationSession) {
        if (session.currentItemIndex < session.items.size - 1) {
            session.currentItemIndex += 1
            session.currentPhase = SessionPhase.PHASE1
            currentItem(session)?.let { it.status = SessionItemStatus.PHASE1 }
        } else {
            session.status = SessionStatus.FINALIZED
            session.finalizedAt = Instant.now()
        }
    }

    // Writes the finalized triple back onto the matching draft leaf through the
    // existing draft-update path (records an undo-log entry, task-076), NOT with
    // ad-hoc SQL.
    private fun writeBackToDraft(
        session: EstimationSession,
        item: SessionItem,
        moderator: User,
        bucketId: UUID? = null
    ) {
        val estimationId = session.estimation?.id ?: return
        val draft = draftRepository.findByEstimationId(estimationId) ?: return
        val beforeDto = draft.toUpdateDto()
        val before = draftVersionMapper.toDomain(draft)

        val targetId = item.nodeLogicalId
        // The item's discussion notes are appended to the leaf's assumptions,
        // prefixed with the session name, so they survive the session (task-129).
        val noteEntry = item.discussionNotes?.takeIf { it.isNotBlank() }?.let { "${session.title}: $it" }
        val newRoots = beforeDto.roots?.map {
            patchFinalizedLeaf(
                it, targetId, item.finalMinEffort!!, item.finalExpectedEffort!!, item.finalMaxEffort!!,
                noteEntry, bucketId
            )
        }
        draftUpdateApplier.apply(draft, beforeDto.copy(roots = newRoots))

        val afterDto = draft.toUpdateDto()
        val after = draftVersionMapper.toDomain(draft)
        undoService.recordMutation(draft, before, after, beforeDto, afterDto, moderator)
    }

    private fun patchFinalizedLeaf(
        node: EstimationNodeUpdateDto,
        targetLogicalId: String?,
        min: Double,
        expected: Double,
        max: Double,
        noteEntry: String?,
        bucketId: UUID? = null
    ): EstimationNodeUpdateDto {
        val patched = if (targetLogicalId != null && node.logicalId?.toString() == targetLogicalId) {
            val assumptions = if (noteEntry != null) {
                Log.debug("Appending session notes to assumptions of leaf $targetLogicalId")
                appendAssumption(node.assumptions, noteEntry)
            } else {
                node.assumptions
            }
            node.copy(
                minEffort = min,
                expectedEffort = expected,
                maxEffort = max,
                assumptions = assumptions,
                // Bucket sessions also decide the leaf's bucket; a null keeps the
                // leaf's existing assignment (PERT sessions never touch it). A
                // leaf that received a sample triple becomes a sample.
                bucketId = bucketId?.toString() ?: node.bucketId,
                isSample = if (bucketId != null) max > 0.0 else node.isSample
            )
        } else {
            node
        }
        return patched.copy(
            children = patched.children.map {
                patchFinalizedLeaf(it, targetLogicalId, min, expected, max, noteEntry, bucketId)
            }
        )
    }

    // Appends a note entry to existing assumptions (newline-separated), never
    // overwriting a pre-existing value.
    private fun appendAssumption(existing: String?, entry: String): String =
        if (existing.isNullOrBlank()) entry else "$existing\n$entry"

    // Votes that count for reveal/finalize: PHASE2 revisions if any, else PHASE1.
    private fun effectiveVotes(item: SessionItem): List<SessionVote> {
        val phase2 = voteRepository.findByItemAndPhase(item.id!!, SessionPhase.PHASE2)
        return phase2.ifEmpty { voteRepository.findByItemAndPhase(item.id!!, SessionPhase.PHASE1) }
    }

    private fun currentItem(session: EstimationSession): SessionItem? =
        session.items.getOrNull(session.currentItemIndex)

    private fun requireSession(id: UUID): EstimationSession =
        sessionRepository.findById(id) ?: throw notFound("Session not found: $id")

    private fun requireModerator(id: UUID, subjectId: String): EstimationSession {
        val session = requireSession(id)
        if (session.moderatorSubjectId != subjectId) {
            throw WebApplicationException("Only the moderator may perform this action", Response.Status.FORBIDDEN)
        }
        return session
    }

    // Every estimation activity — voting, revealing, finalizing — requires a
    // RUNNING session, so a SUSPENDED room is inert rather than quietly
    // accepting work that its participants are not there to do (task-144).
    private fun requireRunning(session: EstimationSession, id: UUID) {
        if (session.status != SessionStatus.RUNNING) throw conflict("Session $id is not RUNNING")
    }

    // The session's estimation method decides how votes reduce to a group
    // estimate. A session without an estimation is a data bug, not a PERT
    // session, so this fails loudly rather than defaulting.
    private fun sessionSupport(session: EstimationSession): EstimationMethodSessionSupport =
        EstimationMethodRegistry.requireSession(session.estimation?.method!!)

    // The only place that assumes the stored votes are a THREE_POINT_PERT
    // triple. session_votes has no bucket column until task-106 extends it, so
    // every persisted vote is still a triple regardless of the estimation's
    // method; this wraps them into the SPI's method-neutral input and unwraps
    // the PERT reduction back out. When bucket voting lands, this is the seam
    // that grows a second branch.
    /**
     * BUCKET_SAMPLED_PERT counterpart of [reducePert]. Wraps the persisted votes
     * into the method's own SPI input and unwraps its reduction.
     *
     * LWW uses `updatedAt ?: createdAt`: a re-vote UPSERTs the existing row, so
     * `createdAt` would pin the estimator's FIRST write and make a later change
     * lose the race it should win.
     */
    private fun reduceBucket(
        support: EstimationMethodSessionSupport,
        votes: List<SessionVote>
    ): BucketSampledSessionResult {
        val inputs = votes.mapNotNull { vote ->
            val bucketId = vote.bucket?.id ?: return@mapNotNull null
            val writtenAt = vote.updatedAt ?: vote.createdAt ?: Instant.EPOCH
            BucketVoteInput(
                assignment = EstimatorBucketAssignment(
                    estimatorId = vote.participantSubjectId ?: "",
                    bucketId = bucketId.toString(),
                    at = KotlinInstant.fromEpochMilliseconds(writtenAt.toEpochMilli())
                ),
                sample = if (vote.isSample == true) {
                    SampleTriple(vote.minEffort, vote.expectedEffort, vote.maxEffort)
                } else {
                    null
                }
            )
        }
        val reduction = support.reduce(inputs)
        return reduction as? BucketSampledSessionResult
            ?: error("Expected a bucket reduction from ${support.method}, got ${reduction::class.simpleName}")
    }

    private fun reducePert(
        support: EstimationMethodSessionSupport,
        votes: List<SessionVote>
    ): VoteAggregate {
        val reduction = support.reduce(
            votes.map { PertVoteInput(EstimatorVote(it.minEffort, it.expectedEffort, it.maxEffort)) }
        )
        val pert = reduction as? PertReduction
            ?: error("Expected a PERT reduction from ${support.method}, got ${reduction::class.simpleName}")
        return pert.aggregate
    }

    private fun buildDto(session: EstimationSession): SessionDto {
        val descriptions = draftLeafDescriptions(session)
        val participantNames = session.participants.associate { it.subjectId to it.displayName }
        // Resolved once per DTO build, not once per item.
        val support = sessionSupport(session)
        return SessionDto(
            id = session.id!!,
            title = session.title ?: "",
            estimationId = session.estimation?.id!!,
            status = session.status,
            moderatorSubjectId = session.moderatorSubjectId ?: "",
            currentItemIndex = session.currentItemIndex,
            currentPhase = session.currentPhase,
            moderatorEstimates = session.moderatorEstimates,
            items = session.items.map { itemDto(it, descriptions, participantNames, support) },
            participants = session.participants.map {
                ParticipantDto(it.subjectId ?: "", it.displayName, it.participantRole, it.agreed)
            }
        )
    }

    private fun itemDto(
        item: SessionItem,
        descriptions: Map<String, String?>,
        participantNames: Map<String?, String?>,
        support: EstimationMethodSessionSupport
    ): SessionItemDto {
        val revealed = item.status == SessionItemStatus.PHASE2 || item.status == SessionItemStatus.FINALIZED
        val votes = effectiveVotes(item)
        val voteDtos = if (revealed) {
            votes.map {
                VoteDto(
                    it.participantSubjectId ?: "",
                    participantNames[it.participantSubjectId],
                    TripleDto(it.minEffort, it.expectedEffort, it.maxEffort),
                    it.phase
                )
            }
        } else {
            null
        }
        // Each method reduces its own vote shape; feeding a bucket session's
        // votes to the PERT reducer would (correctly) blow up in the SPI.
        val isBucketMethod = support.method == EstimationMethod.BUCKET_SAMPLED_PERT
        val aggregate: AggregateDto? = if (revealed && votes.isNotEmpty() && !isBucketMethod) {
            reducePert(support, votes).toDto()
        } else {
            null
        }
        val bucketAssignment: BucketAssignmentDto? = if (revealed && votes.isNotEmpty() && isBucketMethod) {
            reduceBucket(support, votes).assignment?.toDto(participantNames)
        } else {
            null
        }
        val finalTriple = if (item.finalMinEffort != null) {
            TripleDto(item.finalMinEffort!!, item.finalExpectedEffort!!, item.finalMaxEffort!!)
        } else {
            null
        }
        return SessionItemDto(
            nodeLogicalId = item.nodeLogicalId ?: "",
            description = descriptions[item.nodeLogicalId],
            position = item.position,
            status = item.status,
            discussionNotes = item.discussionNotes,
            finalTriple = finalTriple,
            submittedVoteCount = votes.size,
            votes = voteDtos,
            aggregate = aggregate,
            bucketAssignment = bucketAssignment
        )
    }

    private fun AssignmentResult.toDto(participantNames: Map<String?, String?>) = BucketAssignmentDto(
        bucketId = bucketId,
        source = source,
        conflictingAssignments = conflictingAssignments.map {
            AssignmentConflictDto(
                estimatorId = it.estimatorId,
                displayName = participantNames[it.estimatorId],
                bucketId = it.bucketId,
                at = it.at.toString()
            )
        }
    )

    private fun draftLeafDescriptions(session: EstimationSession): Map<String, String?> {
        val estimationId = session.estimation?.id ?: return emptyMap()
        val draft = draftRepository.findByEstimationId(estimationId) ?: return emptyMap()
        val byId = mutableMapOf<String, String?>()
        fun index(node: io.github.theestimator.domain.draft.DraftEstimationNode) {
            if (node !is DraftGroupNode) byId[node.logicalId.toString()] = node.description
            node.children.forEach(::index)
        }
        draft.roots.forEach(::index)
        return byId
    }

    private fun leafLogicalIds(draft: DraftEstimationVersion): Set<String> {
        val ids = mutableSetOf<String>()
        fun index(node: io.github.theestimator.domain.draft.DraftEstimationNode) {
            if (node !is DraftGroupNode) ids.add(node.logicalId.toString())
            node.children.forEach(::index)
        }
        draft.roots.forEach(::index)
        return ids
    }

    private fun notFound(msg: String) = WebApplicationException(msg, Response.Status.NOT_FOUND)
    private fun badRequest(msg: String) = WebApplicationException(msg, Response.Status.BAD_REQUEST)
    private fun conflict(msg: String) = WebApplicationException(msg, Response.Status.CONFLICT)
}
