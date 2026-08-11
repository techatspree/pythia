@file:OptIn(ExperimentalJsExport::class)

package io.pythia.model

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@JsExport
@DomainEntity
data class Project(
    val name: String,
    val description: String = "",
    val client: String = "",
    val status: ProjectStatus = ProjectStatus.ACTIVE,
    val owner: User? = null,
    private val _id: String? = null,
    private val _createdAt: String? = null,
    private val _updatedAt: String? = null
) : BaseDomain(_id, _createdAt, _updatedAt)
