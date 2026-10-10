package io.pythia.auth

import io.quarkus.arc.All
import io.quarkus.logging.Log
import io.quarkus.runtime.LaunchMode
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import org.eclipse.microprofile.config.ConfigProvider

// Fail-closed guard. It refuses to boot on two conditions:
//  1. The dev auth module accepts a forgeable `Authorization: Dev <subjectId>`
//     header (no cryptographic verification), so it must never be active under
//     a NORMAL (production) launch.
//  2. The configured provider has no AuthModule in this build (task-194). Auth
//     beans are gated at BUILD time, but the provider arrives at RUNTIME from the
//     ConfigMap, so the two can disagree — e.g. `dev` against a production build,
//     or a provider no module implements. Such a backend would start and then
//     authenticate nobody; crash-looping with a clear message is the better
//     failure.
@ApplicationScoped
class AuthProviderGuard(
    private val authConfig: AuthConfig,
    // @JvmSuppressWildcards: Kotlin would otherwise emit List<? extends AuthModule>,
    // which is not the injection point type ArC resolves for @All.
    @param:All private val modules: List<@JvmSuppressWildcards AuthModule>
) {

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
        val provider = authConfig.activeProvider
        val moduleNames = modules.map { it.name() }.sorted()
        if (!moduleAvailable(provider, moduleNames)) {
            Log.error(
                "Refusing to start: app.auth.provider='$provider' but this build contains " +
                    "only the auth modules $moduleNames. Set the ConfigMap's APP_AUTH_PROVIDER " +
                    "to a module in the build."
            )
            error(
                "Refusing to start: no auth module '$provider' in this build " +
                    "(modules in build: $moduleNames)"
            )
        }
        // The issuer URL, client id and audience are public identifiers, not
        // secrets. The datasource password IS one and is deliberately never
        // logged here.
        val issuer = configValue("quarkus.oidc.auth-server-url")
        Log.info(
            "Auth provider: $provider (modules in build: $moduleNames, oidc configured: ${issuer != UNSET})"
        )
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

        // The configured provider must name a module that survived build-time
        // gating; otherwise no filter ever populates CurrentUser.
        fun moduleAvailable(provider: String, moduleNames: Collection<String>): Boolean =
            provider in moduleNames
    }
}
