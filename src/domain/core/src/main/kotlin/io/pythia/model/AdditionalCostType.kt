@file:OptIn(ExperimentalJsExport::class)

package io.pythia.model

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@JsExport
enum class AdditionalCostType {
    ONE_TIME,
    RECURRING
}
