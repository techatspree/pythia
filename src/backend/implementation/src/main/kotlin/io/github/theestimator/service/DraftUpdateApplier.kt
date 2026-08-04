package io.github.theestimator.service

import io.github.theestimator.domain.draft.DraftAdditionalCost
import io.github.theestimator.domain.draft.DraftEffortDriver
import io.github.theestimator.domain.draft.DraftEstimationNode
import io.github.theestimator.domain.draft.DraftEstimationParameter
import io.github.theestimator.domain.draft.DraftEstimationVersion
import io.github.theestimator.domain.EstimationBucket
import io.github.theestimator.domain.draft.DraftBucketedItemNode
import io.github.theestimator.domain.draft.DraftFixedItemNode
import io.github.theestimator.domain.draft.DraftGroupNode
import io.github.theestimator.domain.draft.DraftProjectPhase
import io.github.theestimator.domain.draft.DraftTimeRelativeItemNode
import io.github.theestimator.method.EstimationMethod
import io.github.theestimator.rest.dto.AdditionalCostUpdateDto
import io.github.theestimator.rest.dto.BucketUpdateDto
import io.github.theestimator.rest.dto.DraftUpdateDto
import io.github.theestimator.rest.dto.EffortDriverDto
import io.github.theestimator.rest.dto.EstimationNodeUpdateDto
import io.github.theestimator.rest.dto.EstimationParameterDto
import io.github.theestimator.rest.dto.PhaseUpdateDto
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
        update.parameters?.let { applyParameters(draft, it) }
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

    private fun applyParameters(draft: DraftEstimationVersion, params: List<EstimationParameterDto>) {
        draft.parameters.clear()
        params.forEach { dto ->
            draft.parameters.add(DraftEstimationParameter().apply {
                name = dto.name
                value = dto.value
                comment = dto.comment
                version = draft
            })
        }
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
