package io.pythia.domain.session

/** Per-item progress through the two-phase Delphi flow. */
enum class SessionItemStatus {
    PENDING,
    PHASE1,
    PHASE2,
    FINALIZED
}
