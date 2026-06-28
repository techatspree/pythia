package io.github.theestimator.auth.dev

import io.quarkus.arc.properties.IfBuildProperty
import io.quarkus.security.AuthenticationFailedException
import io.quarkus.security.identity.IdentityProviderManager
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.identity.request.AuthenticationRequest
import io.quarkus.security.runtime.QuarkusPrincipal
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.quarkus.vertx.http.runtime.security.ChallengeData
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport
import io.smallrye.mutiny.Uni
import io.vertx.ext.web.RoutingContext
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.Optional

@ApplicationScoped
@IfBuildProperty(name = "app.auth.provider", stringValue = "dev")
class DevHttpAuthMechanism(
    @ConfigProperty(name = "app.auth.dev.default-user")
    private val defaultUserConfig: Optional<String>
) : HttpAuthenticationMechanism {

    private val defaultUser: String
        get() = defaultUserConfig.orElse("").trim()

    override fun authenticate(
        context: RoutingContext,
        identityProviderManager: IdentityProviderManager
    ): Uni<SecurityIdentity> {
        val header = context.request().getHeader("Authorization")
        return when (val result = resolveDevUser(header, defaultUser)) {
            is DevAuthResult.Authenticated -> {
                val identity = QuarkusSecurityIdentity.builder()
                    .setPrincipal(QuarkusPrincipal(result.user.subjectId))
                    .addRoles(result.user.roles.map { it.name }.toSet())
                    .setAnonymous(false)
                    .build()
                Uni.createFrom().item(identity)
            }
            is DevAuthResult.Anonymous -> Uni.createFrom().nullItem()
            is DevAuthResult.Reject -> Uni.createFrom().failure(AuthenticationFailedException())
        }
    }

    override fun getChallenge(context: RoutingContext): Uni<ChallengeData> =
        Uni.createFrom().item(ChallengeData(Response.Status.UNAUTHORIZED.statusCode, null, null))

    override fun getCredentialTypes(): Set<Class<out AuthenticationRequest>> = emptySet()

    override fun getCredentialTransport(context: RoutingContext): Uni<HttpCredentialTransport> =
        Uni.createFrom().nullItem()
}
