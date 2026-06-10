package io.github.theestimator.rest

import io.github.theestimator.auth.CurrentUserProvider
import io.github.theestimator.auth.Role
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

data class CurrentUserDto(
    val subjectId: String,
    val email: String?,
    val displayName: String?,
    val roles: List<Role>,
    val providerName: String
)

@ApplicationScoped
@Path("/auth")
class MeResource(private val currentUserProvider: CurrentUserProvider) {

    @GET
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    fun me(): CurrentUserDto {
        val u = currentUserProvider.get()
        return CurrentUserDto(
            subjectId = u.subjectId,
            email = u.email,
            displayName = u.displayName,
            roles = u.roles.toList(),
            providerName = u.providerName
        )
    }
}
