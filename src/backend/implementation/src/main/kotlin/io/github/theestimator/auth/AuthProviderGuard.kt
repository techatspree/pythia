package io.github.theestimator.auth

import io.quarkus.logging.Log
import io.quarkus.runtime.LaunchMode
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes

// Fail-closed guard: the dev auth module accepts a forgeable
// `Authorization: Dev <subjectId>` header (no cryptographic verification), so
// it must never be active under a NORMAL (production) launch. If it is, refuse
// to boot rather than silently accept forged identities.
@ApplicationScoped
class AuthProviderGuard(private val authConfig: AuthConfig) {

    fun onStart(@Observes event: StartupEvent) {
        val launchMode = LaunchMode.current()
        if (!devProviderAllowed(authConfig.activeProvider, launchMode)) {
            Log.error(
                "Refusing to start: the forgeable 'dev' auth provider is active under a " +
                    "$launchMode launch. Set app.auth.provider=entra for production."
            )
            error(
                "Refusing to start: the forgeable 'dev' auth provider is active under a " +
                    "NORMAL (production) launch — set app.auth.provider=entra"
            )
        }
        Log.info("Auth provider: ${authConfig.activeProvider}")
    }

    companion object {
        // dev auth is only permitted under DEVELOPMENT (quarkusDev) or TEST
        // launches; any other provider is always fine.
        fun devProviderAllowed(provider: String, launchMode: LaunchMode): Boolean =
            provider != "dev" || launchMode != LaunchMode.NORMAL
    }
}
