package io.pythia.testdata

/**
 * The projects the dev fixture seeds, named once (task-176).
 *
 * Both the seeder and the e2e reset endpoint need this list, and they need to
 * agree: the reset deletes every project EXCEPT these, so a name that drifted
 * out of step would wipe the demo data on the next Playwright run.
 *
 * A keep-list rather than a name prefix, deliberately. Most e2e fixtures are
 * called `E2E <something>`, but not all — `endpoint-authorization.test.ts`
 * creates one named literally `EstimatorWrite`, and several specs create
 * projects through the UI. Any prefix rule leaves those behind, which is how
 * the count reached 231 unnoticed.
 */
object SeededProjects {
    const val WEBSHOP = "Webshop Redesign"
    const val MOBILE_APP = "Mobile App MVP"
    const val DATA_PLATFORM = "Data Platform Migration"

    val ALL = listOf(WEBSHOP, MOBILE_APP, DATA_PLATFORM)
}
