package io.pythia.method

/**
 * Per-method behaviour for collaborative estimation sessions — a sibling SPI to
 * [EstimationMethodModule], not a nested part of it: session support is
 * inherently multi-estimator, while the module covers per-leaf calculation and
 * export shaping. Keeping them separate avoids one over-general interface.
 *
 * The signature is method-neutral: each method contributes its own
 * [SessionVoteInput] and [SessionReduction] subtypes from its own package, so a
 * method whose votes are not a three-point triple (bucket+sampled, task-106) can
 * implement this too. See [SessionVoteInput] for why these are marker interfaces
 * rather than type parameters on this interface, and why they are not `sealed`.
 *
 * An implementation that receives an input subtype it does not handle must fail
 * loudly (`error(…)`), consistent with [EstimationMethodRegistry.require] and
 * [EstimationMethodRegistry.requireSession] — never silently filter or return an
 * empty reduction.
 *
 * Domain-internal like [EstimationMethodModule] and [EstimationMethodRegistry]:
 * only the [EstimationMethod] enum is `@JsExport`ed.
 */
interface EstimationMethodSessionSupport {
    val method: EstimationMethod

    /** Reduce the estimators' votes for one work item into the group estimate. */
    fun reduce(votes: List<SessionVoteInput>): SessionReduction
}
