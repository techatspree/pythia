package io.github.theestimator.auth.dev

import io.quarkus.arc.properties.IfBuildProperty
import io.quarkus.security.identity.AuthenticationRequestContext
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.identity.SecurityIdentityAugmentor
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped

/**
 * Placeholder augmentor for the dev module. Runs outside the request
 * scope (during identity construction on the Vert.x event loop), so it
 * cannot safely consult the request-scoped CurrentUserProvider here.
 *
 * task-006 populates CurrentUserProvider via DevAuthFilter (post-matching,
 * with request scope active) and exposes it through provider-agnostic
 * REST resources. @RolesAllowed enforcement against QuarkusSecurityIdentity
 * is out of scope for task-006 — when task-008 adds the first guarded
 * endpoint, this augmentor is the place to translate `CurrentUser.roles`
 * into Quarkus role strings (typically by re-parsing the Authorization
 * header here via an injected RoutingContext, or by switching to an
 * HttpAuthenticationMechanism).
 */
@ApplicationScoped
@IfBuildProperty(name = "app.auth.provider", stringValue = "dev")
class DevSecurityIdentityAugmentor : SecurityIdentityAugmentor {

    override fun augment(
        identity: SecurityIdentity,
        context: AuthenticationRequestContext
    ): Uni<SecurityIdentity> = Uni.createFrom().item(identity)
}
