package io.github.theestimator.model

/**
 * Fallback values used during calculation when an estimation omits the
 * corresponding named parameter. Kept in one place so the draft calculation
 * ([EstimationVersion.calculate]) and the invariant validator agree.
 */
internal object EstimationDefaults {
    /** stdDevFactor — risk std-deviation multiplier. */
    const val STD_DEV_FACTOR = 2.0

    /** dailyRate — daily rate in EUR. */
    const val DAILY_RATE = 800.0

    /** salesSurcharge — sales surcharge fraction. */
    const val SALES_SURCHARGE = 0.1
}
