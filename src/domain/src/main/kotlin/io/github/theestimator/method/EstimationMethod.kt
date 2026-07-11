@file:OptIn(ExperimentalJsExport::class)

package io.github.theestimator.method

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@JsExport
enum class EstimationMethod {
    THREE_POINT_PERT,
    BUCKET_SAMPLED_PERT
}
