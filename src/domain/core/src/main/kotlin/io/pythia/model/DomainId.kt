package io.pythia.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Fresh stable `logicalId` for a domain node.
 *
 * Public rather than `internal` since task-143: every concrete leaf defaults its
 * `logicalId` to this, and the leaves now live in sibling Gradle modules, where
 * `internal` (module-scoped) is invisible.
 */
@OptIn(ExperimentalUuidApi::class)
fun newId(): String = Uuid.random().toString()
