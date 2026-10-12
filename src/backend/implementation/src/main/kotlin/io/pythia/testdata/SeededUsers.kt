package io.pythia.testdata

import io.pythia.auth.CurrentUser

/**
 * Colleagues the dev fixture seeds, so the demo looks like a real team (task-200).
 *
 * They are ordinary `User` rows provisioned with a synthetic subject id. They
 * cannot log in — they are not dev-login users — and exist so that the project
 * owner picker and the "all projects" list show people, not only `Dev …`
 * accounts. Not every colleague owns a project, as in a real team.
 */
object SeededUsers {
    val ANNA_BECKER = colleague("seed:anna.becker", "Anna Becker")
    val JONAS_WEBER = colleague("seed:jonas.weber", "Jonas Weber")
    val LEONIE_SCHULZ = colleague("seed:leonie.schulz", "Leonie Schulz")

    val ALL = listOf(ANNA_BECKER, JONAS_WEBER, LEONIE_SCHULZ)

    private fun colleague(subjectId: String, displayName: String) = CurrentUser(
        subjectId = subjectId,
        email = null,
        displayName = displayName,
        roles = emptySet(),
        providerName = "seed"
    )
}
