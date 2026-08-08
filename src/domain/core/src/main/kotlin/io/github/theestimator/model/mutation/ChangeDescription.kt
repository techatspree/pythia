@file:OptIn(ExperimentalJsExport::class)

package io.github.theestimator.model.mutation

import io.github.theestimator.model.DomainEntity
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

// A closed set of structured, human-readable changes between two EstimationVersion
// snapshots (task-110). Produced by EstimationVersion.diffSummary and shipped to
// the GUI so a history row shows WHAT a PUT changed, not the raw mutation kind.
// Scalar oldValue/newValue are locale-NEUTRAL strings (plain numeric toString);
// the FRONTEND formats numbers per locale via $lib/format.ts.
@JsExport
@DomainEntity
sealed class ChangeDescription {
    abstract val kind: String
}

@JsExport
@DomainEntity
data class ParameterChanged(val name: String, val oldValue: String?, val newValue: String?) : ChangeDescription() {
    override val kind: String = "PARAMETER_CHANGED"
}

@JsExport
@DomainEntity
data class PhaseAdded(val title: String) : ChangeDescription() {
    override val kind: String = "PHASE_ADDED"
}

@JsExport
@DomainEntity
data class PhaseRemoved(val title: String) : ChangeDescription() {
    override val kind: String = "PHASE_REMOVED"
}

@JsExport
@DomainEntity
data class PhaseChanged(
    val title: String,
    val field: String,
    val oldValue: String?,
    val newValue: String?
) : ChangeDescription() {
    override val kind: String = "PHASE_CHANGED"
}

@JsExport
@DomainEntity
data class EffortDriverAdded(val name: String) : ChangeDescription() {
    override val kind: String = "EFFORT_DRIVER_ADDED"
}

@JsExport
@DomainEntity
data class EffortDriverRemoved(val name: String) : ChangeDescription() {
    override val kind: String = "EFFORT_DRIVER_REMOVED"
}

@JsExport
@DomainEntity
data class EffortDriverChanged(
    val name: String,
    val field: String,
    val oldValue: String?,
    val newValue: String?
) : ChangeDescription() {
    override val kind: String = "EFFORT_DRIVER_CHANGED"
}

@JsExport
@DomainEntity
data class AdditionalCostAdded(val title: String) : ChangeDescription() {
    override val kind: String = "ADDITIONAL_COST_ADDED"
}

@JsExport
@DomainEntity
data class AdditionalCostRemoved(val title: String) : ChangeDescription() {
    override val kind: String = "ADDITIONAL_COST_REMOVED"
}

@JsExport
@DomainEntity
data class AdditionalCostChanged(
    val title: String,
    val field: String,
    val oldValue: String?,
    val newValue: String?
) : ChangeDescription() {
    override val kind: String = "ADDITIONAL_COST_CHANGED"
}

@JsExport
@DomainEntity
data class NodeAdded(val path: String, val nodeType: String) : ChangeDescription() {
    override val kind: String = "NODE_ADDED"
}

@JsExport
@DomainEntity
data class NodeRemoved(val path: String, val nodeType: String) : ChangeDescription() {
    override val kind: String = "NODE_REMOVED"
}

@JsExport
@DomainEntity
data class NodeRenamed(val pathBefore: String, val oldTitle: String, val newTitle: String) : ChangeDescription() {
    override val kind: String = "NODE_RENAMED"
}

@JsExport
@DomainEntity
data class NodeMoved(val pathBefore: String, val pathAfter: String) : ChangeDescription() {
    override val kind: String = "NODE_MOVED"
}

@JsExport
@DomainEntity
data class NodeValueChanged(
    val path: String,
    val field: String,
    val oldValue: String?,
    val newValue: String?
) : ChangeDescription() {
    override val kind: String = "NODE_VALUE_CHANGED"
}

@JsExport
@DomainEntity
data class NotesChanged(val oldValue: String?, val newValue: String?) : ChangeDescription() {
    override val kind: String = "NOTES_CHANGED"
}

@JsExport
@DomainEntity
data class OtherChange(val otherKind: String) : ChangeDescription() {
    override val kind: String = "OTHER_CHANGE"
}

@JsExport
@DomainEntity
data class Truncated(val remaining: Int) : ChangeDescription() {
    override val kind: String = "TRUNCATED"
}
