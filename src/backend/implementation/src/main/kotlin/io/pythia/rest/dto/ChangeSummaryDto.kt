package io.pythia.rest.dto

// One structured change within a mutation-log entry's summary (task-110). A flat
// DTO: only the fields relevant to `kind` are populated; the frontend switches on
// `kind`. `kind` matches the domain `ChangeDescription` variant name (e.g.
// "PARAMETER_CHANGED", "NODE_VALUE_CHANGED", "TRUNCATED").
data class ChangeSummaryDto(
    val kind: String,
    val path: String? = null,
    val field: String? = null,
    val oldValue: String? = null,
    val newValue: String? = null,
    val nodeType: String? = null,
    val remaining: Int? = null
)
