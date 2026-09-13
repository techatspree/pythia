package io.pythia.service

import io.pythia.domain.submitted.SubmittedBucketedItemNode
import io.pythia.domain.submitted.SubmittedEstimationNode
import io.pythia.method.bucketsampled.BucketedEstimationItem
import io.pythia.method.threepoint.FixedEstimationItem
import io.pythia.model.EstimationItem

/**
 * Maps ONE persisted leaf of a submitted version onto the KMP domain item that
 * the estimation-method SPI speaks (task-107).
 *
 * It exists so the export can ask the method module for its own cells
 * (`EstimationMethodModule.exportRow`) instead of rebuilding each method's column
 * shape in the backend — which is exactly what the SPI is there to prevent. The
 * exporters walk `SubmittedEstimationNode`, a JPA entity, while `exportRow` takes
 * an `EstimationItem`, so something has to bridge the two; mapping persisted
 * state onto the domain is the backend's job.
 *
 * Importing both method modules here is allowed: the compiler-enforced ban is on
 * CROSS-METHOD imports inside the domain, and the backend consumes the `:domain`
 * aggregator, which exposes all of them.
 *
 * Only the fields the export shapes read are populated — this is not a
 * general-purpose rehydration of a submitted tree, and the calculated values
 * (mean, offerPT) are taken from the persisted node by the exporters themselves.
 */
object SubmittedItemMapper {

    fun toDomain(node: SubmittedEstimationNode): EstimationItem =
        if (node is SubmittedBucketedItemNode) {
            BucketedEstimationItem(
                bucketId = node.bucket?.id?.toString() ?: "",
                isSample = node.isSample ?: false,
                optimistic = node.minEffort,
                likely = node.expectedEffort,
                pessimistic = node.maxEffort,
                _description = node.description ?: ""
            )
        } else {
            FixedEstimationItem(
                _description = node.description ?: "",
                _minEffort = node.minEffort ?: 0.0,
                _expectedEffort = node.expectedEffort ?: 0.0,
                _maxEffort = node.maxEffort ?: 0.0
            )
        }
}
