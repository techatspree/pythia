@file:OptIn(ExperimentalJsExport::class)

package io.pythia.model.mutation

import io.pythia.model.DomainEntity
import io.pythia.model.EstimationVersion
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@JsExport
@DomainEntity
sealed class DraftMutation {
    abstract val kind: String

    abstract fun apply(current: EstimationVersion): EstimationVersion

    abstract fun inverse(): DraftMutation
}

@JsExport
@DomainEntity
data class ReplaceWholeDraft(
    val before: EstimationVersion,
    val after: EstimationVersion
) : DraftMutation() {
    override val kind: String = "REPLACE_WHOLE_DRAFT"

    override fun apply(current: EstimationVersion): EstimationVersion = after

    override fun inverse(): DraftMutation = ReplaceWholeDraft(before = after, after = before)
}
