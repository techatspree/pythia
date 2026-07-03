package io.github.theestimator.model.mutation

import io.github.theestimator.model.EstimationVersion
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

fun EstimationVersion.diff(other: EstimationVersion): DraftMutation? {
    if (this == other) {
        logger.debug { "diff: versions structurally equal, no mutation produced" }
        return null
    }
    logger.debug { "diff: versions differ, producing ReplaceWholeDraft" }
    return ReplaceWholeDraft(this, other)
}
