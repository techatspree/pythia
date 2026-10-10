package io.pythia.auth.entra

import io.pythia.auth.AuthConfig
import io.pythia.auth.AuthModule
import io.quarkus.arc.properties.UnlessBuildProperty
import jakarta.enterprise.context.ApplicationScoped

// Deliberately asymmetric with the dev module (task-194). Entra is present in
// EVERY build that is not explicitly a dev build, including a %prod build where
// app.auth.provider is missing at augmentation (it arrives at runtime from the
// ConfigMap, task-161). The dev beans keep their IfBuildProperty gate on "dev",
// which requires the explicit value, so a production build can never contain the
// forgeable module.
@ApplicationScoped
@UnlessBuildProperty(name = "app.auth.provider", stringValue = "dev", enableIfMissing = true)
class EntraAuthModule(private val authConfig: AuthConfig) : AuthModule {
    override fun name(): String = "entra"
    override fun isActive(): Boolean = authConfig.activeProvider == "entra"
}
