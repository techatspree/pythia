package io.pythia.auth

import jakarta.enterprise.context.RequestScoped

@RequestScoped
class CurrentUserProvider {
    var current: CurrentUser? = null

    fun get(): CurrentUser =
        current ?: error("No AuthModule populated CurrentUser for this request")
}
