package io.pythia.auth

import io.quarkus.logging.Log
import io.quarkus.runtime.LaunchMode
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import org.eclipse.microprofile.config.ConfigProvider

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
        // The issuer URL, client id and audience are public identifiers, not
        // secrets. The datasource password IS one and is deliberately never
        // logged here.
        val issuer = configValue("quarkus.oidc.auth-server-url")
        Log.info("Auth provider: ${authConfig.activeProvider} (oidc configured: ${issuer != UNSET})")
        Log.debug(
            "OIDC issuer=$issuer clientId=${configValue("quarkus.oidc.client-id")} " +
                "audience=${configValue("quarkus.oidc.token.audience")}"
        )
    }

    // Read OPTIONALLY, never injected. The quarkus.oidc.* keys belong to a
    // config mapping that Quarkus validates as required, so a @ConfigProperty
    // for one of them fails startup in %dev/%test where OIDC is not configured
    // at all — even with a defaultValue.
    private fun configValue(name: String): String =
        ConfigProvider.getConfig()
            .getOptionalValue(name, String::class.java)
            .orElse("")
            .ifBlank { UNSET }

    companion object {
        private const val UNSET = "<unset>"

        // dev auth is only permitted under DEVELOPMENT (quarkusDev) or TEST
        // launches; any other provider is always fine.
        fun devProviderAllowed(provider: String, launchMode: LaunchMode): Boolean =
            provider != "dev" || launchMode != LaunchMode.NORMAL
    }
}
