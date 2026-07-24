package io.github.theestimator.domain.session

/** Coarse lifecycle of a collaborative estimation session. Per-item progress is
 *  tracked separately by SessionItem.status + the session's
 *  current_item_index/current_phase. */
enum class SessionStatus {
    CREATED,
    RUNNING,
    FINALIZED,
    CANCELLED
}
