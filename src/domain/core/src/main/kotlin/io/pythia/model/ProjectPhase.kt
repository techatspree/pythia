@file:OptIn(ExperimentalJsExport::class)

package io.pythia.model

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/**
 * Where a phase's length comes from (task-177). Per phase, not global: a plan
 * commonly pins one phase to a contractual length and derives the rest.
 */
@JsExport
enum class PhaseDurationMode { EXPLICIT, AUTOMATIC }

@JsExport
@DomainEntity
data class ProjectPhase(
    val name: String,
    val abbreviation: String,
    val durationWeeks: Double = 0.0,
    /**
     * Whether `durationWeeks` was typed in or derived from the schedule
     * (task-177). Defaults to EXPLICIT so a phase constructed without one — and
     * every version persisted before this field existed — behaves exactly as
     * before.
     *
     * Positioned after `durationWeeks` and BEFORE the private `_id` /
     * `_createdAt` / `_updatedAt` params on purpose: several sites construct
     * this phase positionally with three arguments, and a default-valued field
     * ahead of those trailing params keeps them compiling.
     */
    val durationMode: PhaseDurationMode = PhaseDurationMode.EXPLICIT,
    private val _id: String? = null,
    private val _createdAt: String? = null,
    private val _updatedAt: String? = null
) : BaseDomain(_id, _createdAt, _updatedAt)
