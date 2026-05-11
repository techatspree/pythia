@file:OptIn(ExperimentalJsExport::class)

package io.github.theestimator.model

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@JsExport
@DomainEntity
data class User(
    val entraSubjectId: String? = null,
    val displayName: String = "",
    private val _id: String? = null,
    private val _createdAt: String? = null,
    private val _updatedAt: String? = null
) : BaseDomain(_id, _createdAt, _updatedAt)
