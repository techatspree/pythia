@file:OptIn(ExperimentalJsExport::class)

package io.github.theestimator.model

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@JsExport
@DomainEntity
abstract class BaseDomain(
    val id: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
