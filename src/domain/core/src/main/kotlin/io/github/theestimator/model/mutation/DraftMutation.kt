@file:OptIn(ExperimentalJsExport::class)

package io.github.theestimator.model.mutation

import io.github.theestimator.model.DomainEntity
import io.github.theestimator.model.EstimationVersion
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
