package io.github.theestimator.domain.session

/** Coarse lifecycle of a collaborative estimation session. Per-item progress is
 *  tracked separately by SessionItem.status + the session's
 *  current_item_index/current_phase.
 *
 *  CREATED → RUNNING → (SUSPENDED ⇄ RUNNING) → FINALIZED | ENDED_EARLY,
 *  with CANCELLED as the abandon exit from anywhere.
 *
 *  FINALIZED and ENDED_EARLY both keep their results — every item finalized
 *  along the way was already written back to the draft leaf. They differ only
 *  in whether the last item was reached. CANCELLED means "this session was a
 *  mistake"; it is not an early finish. */
enum class SessionStatus {
    CREATED,
    RUNNING,
    SUSPENDED,
    FINALIZED,
    ENDED_EARLY,
    CANCELLED
}
