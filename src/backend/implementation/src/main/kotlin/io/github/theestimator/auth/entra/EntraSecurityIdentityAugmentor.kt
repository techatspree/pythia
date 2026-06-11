package io.github.theestimator.auth.entra

import io.quarkus.arc.properties.IfBuildProperty
import io.quarkus.security.identity.AuthenticationRequestContext
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.identity.SecurityIdentityAugmentor
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
@IfBuildProperty(name = "app.auth.provider", stringValue = "entra")
class EntraSecurityIdentityAugmentor : SecurityIdentityAugmentor {

    override fun augment(
        identity: SecurityIdentity,
        context: AuthenticationRequestContext
    ): Uni<SecurityIdentity> {
        if (identity.isAnonymous) return Uni.createFrom().item(identity)
        val mapped = entraRolesToDomain(identity.roles).map { it.name }.toSet()
        if (mapped.isEmpty()) return Uni.createFrom().item(identity)
        return Uni.createFrom().item(
            QuarkusSecurityIdentity.builder(identity).addRoles(mapped).build()
        )
    }
}
