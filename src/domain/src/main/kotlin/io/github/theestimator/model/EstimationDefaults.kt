package io.github.theestimator.model

/**
 * Fallback values used during calculation when an estimation omits the
 * corresponding named parameter. Kept in one place so the draft calculation
 * ([EstimationVersion.calculate]) and the invariant validator agree.
 */
internal object EstimationDefaults {
    /** Standardabweichungsfaktor — risk std-deviation multiplier. */
    const val STD_DEV_FACTOR = 2.0

    /** Tagessatz — daily rate in EUR. */
    const val DAILY_RATE = 800.0

    /** Vertriebszuschlag — sales surcharge fraction. */
    const val SALES_SURCHARGE = 0.1
}
