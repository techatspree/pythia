package io.github.theestimator.auth

data class CurrentUser(
    val subjectId: String,
    val email: String?,
    val displayName: String?,
    val roles: Set<Role>,
    val providerName: String
)
