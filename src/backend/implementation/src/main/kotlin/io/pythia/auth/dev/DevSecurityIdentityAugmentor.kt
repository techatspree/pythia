package io.pythia.auth.dev

import io.quarkus.arc.properties.IfBuildProperty
import io.quarkus.security.identity.AuthenticationRequestContext
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.identity.SecurityIdentityAugmentor
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped

/**
 * No-op pass-through. The dev module builds its `SecurityIdentity`
 * (principal + roles) inside `DevHttpAuthMechanism`, which is the
 * standard Quarkus pattern for custom auth. This augmentor stays
 * registered so the SPI shape mirrors the Entra side, but it
 * doesn't enrich the identity further. The Quarkus role strings
 * `VIEWER` / `ESTIMATOR` / `ADMIN` are already populated by the
 * mechanism, so `@RolesAllowed` works against `DevHttpAuthMechanism`
 * output without any post-processing.
 */
@ApplicationScoped
@IfBuildProperty(name = "app.auth.provider", stringValue = "dev")
class DevSecurityIdentityAugmentor : SecurityIdentityAugmentor {

    override fun augment(
        identity: SecurityIdentity,
        context: AuthenticationRequestContext
    ): Uni<SecurityIdentity> {
        return Uni.createFrom().item(
            QuarkusSecurityIdentity.builder(identity).build()
        )
    }
}
