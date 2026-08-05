package io.github.theestimator.model

import kotlin.js.JsExport

/**
 * The default values of the three hardwired calculation inputs (task-138).
 *
 * These are typed fields on [EstimationVersion], not user-named rows, so a GUI
 * rename can no longer make a lookup miss and silently fall back to a default.
 *
 * Public and `@JsExport`ed on purpose: the backend entities seed their columns
 * from here and the frontend parameter panel offers them as "use defaults", so
 * all three tiers read ONE set of numbers instead of re-hardcoding them.
 */
@JsExport
object EstimationDefaults {
    /** Risk std-deviation multiplier. */
    const val STD_DEV_FACTOR = 2.0

    /** Daily rate in EUR. */
    const val DAILY_RATE = 800.0

    /** Sales surcharge as a fraction of the offer total. */
    const val SALES_SURCHARGE = 0.1
}
