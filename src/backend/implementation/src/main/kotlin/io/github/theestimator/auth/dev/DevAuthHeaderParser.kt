package io.github.theestimator.auth.dev

private val SUBJECT_ID_REGEX = Regex("^[a-z][a-z0-9-]*$")

sealed interface DevAuthResult {
    /** Header missing AND default-user is empty/strict; reject with 401. */
    object Anonymous : DevAuthResult

    /** Header present but invalid (wrong scheme, malformed subject, unknown user). */
    object Reject : DevAuthResult

    data class Authenticated(val user: DevUser) : DevAuthResult
}

fun resolveDevUser(authorizationHeader: String?, defaultUser: String): DevAuthResult {
    if (authorizationHeader.isNullOrBlank()) {
        if (defaultUser.isBlank()) return DevAuthResult.Anonymous
        val u = DEV_USERS[defaultUser] ?: return DevAuthResult.Reject
        return DevAuthResult.Authenticated(u)
    }
    val parts = authorizationHeader.split(Regex("\\s+"), limit = 2)
    if (parts.size != 2 || parts[0] != "Dev") return DevAuthResult.Reject
    val subjectId = parts[1].trim()
    if (!SUBJECT_ID_REGEX.matches(subjectId)) return DevAuthResult.Reject
    val u = DEV_USERS[subjectId] ?: return DevAuthResult.Reject
    return DevAuthResult.Authenticated(u)
}
