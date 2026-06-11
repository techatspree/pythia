package io.github.theestimator.auth.entra

import io.github.theestimator.auth.AuthConfig
import io.github.theestimator.auth.AuthModule
import io.quarkus.arc.properties.IfBuildProperty
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
@IfBuildProperty(name = "app.auth.provider", stringValue = "entra")
class EntraAuthModule(private val authConfig: AuthConfig) : AuthModule {
    override fun name(): String = "entra"
    override fun isActive(): Boolean = authConfig.activeProvider == "entra"
}
