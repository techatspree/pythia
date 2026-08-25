package io.pythia.service

import io.pythia.domain.draft.DraftAdditionalCost
import io.pythia.domain.draft.DraftEffortDriver
import io.pythia.domain.draft.DraftEstimationNode
import io.pythia.domain.draft.DraftEstimationVersion
import io.pythia.domain.EstimationBucket
import io.pythia.domain.draft.DraftBucketedItemNode
import io.pythia.domain.draft.DraftFixedItemNode
import io.pythia.domain.draft.DraftGroupNode
import io.pythia.domain.draft.DraftProjectPhase
import io.pythia.domain.draft.DraftScheduleDependency
import io.pythia.domain.draft.DraftTimeRelativeItemNode
import io.pythia.method.EstimationMethod
import io.pythia.rest.dto.AdditionalCostUpdateDto
import io.pythia.rest.dto.BucketUpdateDto
import io.pythia.rest.dto.DraftUpdateDto
import io.pythia.rest.dto.EffortDriverDto
import io.pythia.rest.dto.EstimationNodeUpdateDto
import io.pythia.rest.dto.PhaseUpdateDto
import io.pythia.rest.dto.ScheduleDependencyDto
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.BadRequestException
import java.util.UUID

// Shared clear-and-rebuild that writes a DraftUpdateDto into a draft entity.
// The canonical draft-write path used by both the REST PUT (updateDraft) and
// the Undo service (restoring a captured snapshot).
@ApplicationScoped
class DraftUpdateApplier {

    @jakarta.persistence.PersistenceContext
    lateinit var entityManager: jakarta.persistence.EntityManager

    fun apply(draft: DraftEstimationVersion, update: DraftUpdateDto) {
        update.notes?.let { draft.notes = it }
        update.dailyRate?.let { draft.dailyRate = it }
        update.stdDevFactor?.let { draft.stdDevFactor = it }
        update.salesSurcharge?.let { draft.salesSurcharge = it }
        update.teamFte?.let { draft.teamFte = it }
        update.dependencies?.let { applyScheduleDependencies(draft, it) }
        update.effortDrivers?.let { applyEffortDrivers(draft, it) }
        update.phases?.let { applyPhases(draft, it) }
        // Buckets before roots: a bucketed leaf resolves its bucket by id.
        update.buckets?.let { applyBuckets(draft, it) }
        update.roots?.let { applyRoots(draft, it) }
        update.additionalCosts?.let { applyAdditionalCosts(draft, it) }
    }

    private fun applyBuckets(draft: DraftEstimationVersion, bucketDtos: List<BucketUpdateDto>) {
        val estimation = draft.estimation ?: return
        // Upsert by id (not clear-and-rebuild): node rows FK-reference buckets
        // with ON DELETE RESTRICT, so orphan-removing a referenced bucket would
        // fail. Ids are client-assigned, so the snapshot round-trips undo/redo.
        val keptIds = bucketDtos.mapNotNull { it.id }.toSet()
        estimation.buckets.removeAll { it.id !in keptIds }
        val byId = estimation.buckets.associateBy { it.id }

        // Reordering swaps positions, and these are applied as row-by-row
        // UPDATEs. `estimation_buckets` has UNIQUE(estimation_id, position),
        // which PostgreSQL evaluates after EVERY statement, so the transient
        // state mid-swap ("two buckets briefly at position 0") aborted the whole
        // transaction — the user saw "could not be saved" and lost the reorder.
        // Park every existing bucket on a temporary NEGATIVE position first and
        // flush, so no final position can collide with a stale one. Negatives
        // are never handed out as real positions, so the parking slots are
        // collision-free among themselves too.
        // (V15 additionally makes the constraint DEFERRABLE in the Flyway-managed
        // schemas; this keeps the write correct even where the constraint is
        // immediate — e.g. the Hibernate-generated dev/test schema.)
        if (byId.isNotEmpty()) {
            estimation.buckets.forEachIndexed { idx, bucket -> bucket.position = -(idx + 1) }
            entityManager.flush()
        }

        bucketDtos.forEach { dto ->
            val existing = dto.id?.let { byId[it] }
            if (existing != null) {
                existing.position = dto.position
                existing.label = dto.label
            } else {
                estimation.buckets.add(EstimationBucket().apply {
                    id = dto.id
                    this.estimation = estimation
                    position = dto.position
                    label = dto.label
                })
            }
        }
    }

    // Clear-and-rebuild like the other collections. An edge naming a logicalId
    // that is not a current root is ACCEPTED: task-155 ignores unknown ids when
    // it computes, so rejecting here would make a root deletion fail a later
    // save. Duplicates are stopped by the UNIQUE constraint, not here.
    private fun applyScheduleDependencies(
        draft: DraftEstimationVersion,
        dependencies: List<ScheduleDependencyDto>
    ) {
        // Clear, then FLUSH before re-inserting. Hibernate orders INSERTs ahead
        // of orphan-removal DELETEs, and UNIQUE(version_id, from, to) is
        // evaluated by PostgreSQL after every statement — so re-saving an
        // UNCHANGED edge list (the common case) transiently held two copies of
        // the same edge and aborted the transaction. Same trap as the bucket
        // reorder above; here a plain flush is enough, since this is a pure
        // clear-and-rebuild rather than an in-place swap.
        if (draft.scheduleDependencies.isNotEmpty()) {
            draft.scheduleDependencies.clear()
            entityManager.flush()
        }
        dependencies.forEach { dto ->
            draft.scheduleDependencies.add(DraftScheduleDependency().apply {
                fromLogicalId = dto.fromLogicalId
                toLogicalId = dto.toLogicalId
                version = draft
            })
            Log.debug("Schedule edge ${dto.fromLogicalId} -> ${dto.toLogicalId} on draft ${draft.id}")
        }
        Log.info("Schedule applied: draft=${draft.id} teamFte=${draft.teamFte} edges=${dependencies.size}")
    }

    private fun applyEffortDrivers(draft: DraftEstimationVersion, drivers: List<EffortDriverDto>) {
        draft.effortDrivers.clear()
        drivers.forEach { dto ->
            draft.effortDrivers.add(DraftEffortDriver().apply {
                description = dto.description
                factor = dto.factor
                comment = dto.comment
                version = draft
            })
        }
    }

    private fun applyPhases(draft: DraftEstimationVersion, phaseDtos: List<PhaseUpdateDto>) {
        // Upsert by abbreviation rather than clear-and-rebuild: persistent
        // DraftEstimationNode rows hold Java references to phase entities
        // by object identity, so orphan-removing them would leave dangling
        // references that fail Hibernate's pre-flush transient-reference
        // check (see EstimationVersionResourceIT "PUT replacing only phases
        // while persistent nodes reference them …").
        val keptAbbreviations = phaseDtos.map { it.abbreviation }.toSet()
        draft.phases.removeAll { it.abbreviation !in keptAbbreviations }
        val byAbbr = draft.phases.associateBy { it.abbreviation }
        phaseDtos.forEach { dto ->
            val existing = byAbbr[dto.abbreviation]
            if (existing != null) {
                existing.name = dto.name
                existing.durationWeeks = dto.durationWeeks
            } else {
                draft.phases.add(DraftProjectPhase().apply {
                    name = dto.name
                    abbreviation = dto.abbreviation
                    durationWeeks = dto.durationWeeks
                    version = draft
                })
            }
        }
    }

    private fun applyRoots(draft: DraftEstimationVersion, rootDtos: List<EstimationNodeUpdateDto>) {
        draft.roots.clear()
        rootDtos.forEachIndexed { idx, dto ->
            draft.roots.add(buildDraftNode(draft, dto, null, idx))
        }
    }

    private fun buildDraftNode(
        draft: DraftEstimationVersion,
        dto: EstimationNodeUpdateDto,
        parentNode: DraftEstimationNode?,
        pos: Int
    ): DraftEstimationNode {
        val node: DraftEstimationNode = when (dto.type) {
            "GROUP" -> DraftGroupNode().apply { title = dto.title }
            "TIME_RELATIVE" -> DraftTimeRelativeItemNode().apply { unit = dto.unit ?: "h/Woche" }
            "BUCKETED" -> buildBucketedNode(draft, dto)
            else -> DraftFixedItemNode()
        }
        node.apply {
            logicalId = dto.logicalId ?: UUID.randomUUID()
            position = pos
            version = draft
            parent = parentNode
            if (dto.type != "GROUP") {
                description = dto.description
                code = dto.code
                minEffort = dto.minEffort
                expectedEffort = dto.expectedEffort
                maxEffort = dto.maxEffort
                assumptions = dto.assumptions
                phase = dto.phaseAbbreviation?.let { abbr ->
                    draft.phases.find { it.abbreviation == abbr }
                }
            }
        }
        dto.children.forEachIndexed { idx, childDto ->
            node.children.add(buildDraftNode(draft, childDto, node, idx))
        }
        return node
    }

    private fun buildBucketedNode(draft: DraftEstimationVersion, dto: EstimationNodeUpdateDto): DraftBucketedItemNode {
        // A BUCKETED leaf is only valid when the owning estimation opted into the
        // bucket + sampled method; otherwise it is a client/method mismatch (400).
        if (draft.estimation?.method != EstimationMethod.BUCKET_SAMPLED_PERT) {
            Log.warn(
                "Rejected BUCKETED leaf ${dto.logicalId}: estimation method is " +
                    "${draft.estimation?.method}, not BUCKET_SAMPLED_PERT"
            )
            throw BadRequestException("BUCKETED leaves are only allowed for a BUCKET_SAMPLED_PERT estimation")
        }
        val bucketUuid = dto.bucketId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        val bucket = bucketUuid?.let { uuid -> draft.estimation?.buckets?.find { it.id == uuid } }
            ?: run {
                Log.warn("Rejected BUCKETED leaf ${dto.logicalId}: unknown bucketId ${dto.bucketId}")
                throw BadRequestException("Unknown bucketId ${dto.bucketId} for BUCKETED leaf")
            }
        // min/expected/max (the sample triple) + assumptions are copied by the
        // shared non-GROUP block in buildDraftNode.
        return DraftBucketedItemNode().apply {
            this.bucket = bucket
            this.isSample = dto.isSample
        }
    }

    private fun applyAdditionalCosts(draft: DraftEstimationVersion, costDtos: List<AdditionalCostUpdateDto>) {
        draft.additionalCosts.clear()
        costDtos.forEach { dto ->
            val costPhase = dto.phaseAbbreviation?.let { abbr ->
                draft.phases.find { it.abbreviation == abbr }
            }
            draft.additionalCosts.add(DraftAdditionalCost().apply {
                description = dto.description
                amount = dto.amount
                type = dto.type
                amountPerWeek = dto.amountPerWeek
                phase = costPhase
                version = draft
            })
        }
    }
}
