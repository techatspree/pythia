package io.pythia.auth.dev

import io.pythia.auth.AuthConfig
import io.pythia.auth.AuthModule
import io.quarkus.arc.properties.IfBuildProperty
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
@IfBuildProperty(name = "app.auth.provider", stringValue = "dev")
class DevAuthModule(private val authConfig: AuthConfig) : AuthModule {
    override fun name(): String = "dev"
    override fun isActive(): Boolean = authConfig.activeProvider == "dev"
}
