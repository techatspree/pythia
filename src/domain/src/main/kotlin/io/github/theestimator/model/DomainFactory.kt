@file:OptIn(ExperimentalJsExport::class)

package io.github.theestimator.model

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@JsExport
fun createFixedItem(
    description: String,
    minEffort: Double = 0.0,
    expectedEffort: Double = 0.0,
    maxEffort: Double = 0.0,
    assumptions: String = "",
    logicalId: String = newId()
): FixedEstimationItem = FixedEstimationItem(
    _description = description,
    _minEffort = minEffort,
    _expectedEffort = expectedEffort,
    _maxEffort = maxEffort,
    _assumptions = assumptions,
    _logicalId = logicalId
)

@JsExport
fun createTimeRelativeItem(
    description: String,
    unit: String = "h/Woche",
    minEffort: Double = 0.0,
    expectedEffort: Double = 0.0,
    maxEffort: Double = 0.0,
    assumptions: String = "",
    logicalId: String = newId(),
    phase: ProjectPhase? = null
): TimeRelativeEstimationItem = TimeRelativeEstimationItem(
    unit = unit,
    _description = description,
    _minEffort = minEffort,
    _expectedEffort = expectedEffort,
    _maxEffort = maxEffort,
    _assumptions = assumptions,
    _logicalId = logicalId,
    _phase = phase
)

@JsExport
fun createGroup(
    title: String,
    logicalId: String = newId(),
    items: Array<EstimationItem> = emptyArray()
): EstimationItemGroup = EstimationItemGroup(
    title = title,
    logicalId = logicalId,
    items = items.toList()
)

@JsExport
fun createVersion(
    versionNumber: Int,
    isDraft: Boolean,
    notes: String = "",
    parameters: Array<EstimationParameter> = emptyArray(),
    effortDrivers: Array<EffortDriver> = emptyArray(),
    phases: Array<ProjectPhase> = emptyArray(),
    itemGroups: Array<EstimationItemGroup> = emptyArray()
): EstimationVersion = EstimationVersion(
    versionNumber = versionNumber,
    status = if (isDraft) EstimationVersionStatus.DRAFT else EstimationVersionStatus.SUBMITTED,
    notes = notes,
    parameters = parameters.toList(),
    effortDrivers = effortDrivers.toList(),
    phases = phases.toList(),
    roots = itemGroups.map { g ->
        EstimationGroup(title = g.title, children = g.items, _logicalId = g.logicalId)
    }
)
