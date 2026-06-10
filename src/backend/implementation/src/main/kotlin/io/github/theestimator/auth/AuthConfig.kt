package io.github.theestimator.auth

import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
class AuthConfig(
    @ConfigProperty(name = "app.auth.provider") val activeProvider: String
)
