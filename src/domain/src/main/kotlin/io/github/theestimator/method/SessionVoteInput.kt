package io.github.theestimator.method

/**
 * One estimator's contribution to a collaborative session, in whatever shape the
 * estimation method votes in. Each method contributes its own subtype from its
 * own package (PERT: `PertVoteInput`; bucket+sampled: task-106).
 *
 * ## Why marker types rather than a generic SPI
 *
 * The obvious alternative is `EstimationMethodSessionSupport<I, R>`. It does not
 * work here: [EstimationMethodRegistry] stores every support in ONE
 * `Map<EstimationMethod, EstimationMethodSessionSupport>`, so a generic interface
 * erases to `*` inside that map and `requireSession(method)` hands back a
 * star-projected type. Every call site would then need an unchecked cast and the
 * compiler could no longer check the input/result pairing at all. With marker
 * types the map stays homogeneous and type-safe; the cost is one checked cast on
 * the result at the call site, which fails loudly instead of silently.
 *
 * ## Why NOT `sealed`
 *
 * Sealing this would be the natural next step — it would make a `when` over the
 * subtypes exhaustive. Kotlin forbids it here: **a sealed type's direct subtypes
 * must live in the same package**, but each method owns its own package
 * (`…method.threepoint`, `…method.bucketsampled`) per the "one Kotlin package
 * per method" rule, so `sealed` and that rule are mutually exclusive. The
 * package rule is the load-bearing architectural constraint, so these stay
 * plain interfaces. Do not "tighten" them to `sealed` — it does not compile
 * ("A class can only extend a sealed class or interface declared in the same
 * package"). Exhaustiveness is replaced by the SPI contract that an
 * implementation must `error(…)` on an input subtype it does not handle.
 *
 * Domain-internal — deliberately not `@JsExport`ed. The method's own vote types
 * (`EstimatorVote` / `VoteAggregate`) stay exported and are WRAPPED by these
 * subtypes rather than implementing them, so the generated `domain.d.mts`
 * surface is unaffected.
 */
interface SessionVoteInput

/**
 * The group result a method's session reduction produces. Companion to
 * [SessionVoteInput]; see that KDoc for why these are plain marker interfaces
 * rather than type parameters on [EstimationMethodSessionSupport], and why they
 * are not `sealed`.
 */
interface SessionReduction
