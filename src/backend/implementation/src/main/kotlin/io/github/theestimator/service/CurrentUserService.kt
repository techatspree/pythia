package io.github.theestimator.service

import io.github.theestimator.auth.CurrentUser
import io.github.theestimator.domain.User
import io.github.theestimator.repository.UserRepository
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

// Just-in-time provisioning: every authenticated principal (dev or Entra) is
// find-or-created as a persisted User keyed by the unique entraSubjectId, so
// User FKs (audit / ownership / the undo log) always resolve. Provider-agnostic
// and takes the principal as a parameter (pure to test; no DB writes on the
// Vert.x event loop).
@ApplicationScoped
class CurrentUserService(private val userRepository: UserRepository) {

    @Transactional
    fun ensureUser(principal: CurrentUser): User {
        // display_name is @NotBlank; fall back to the subject id when the
        // principal carries no display name.
        val name = principal.displayName ?: principal.subjectId
        val existing = userRepository.findByEntraSubjectId(principal.subjectId)
        if (existing != null) {
            if (existing.displayName != name) existing.displayName = name
            return existing
        }
        val user = User().apply {
            entraSubjectId = principal.subjectId
            displayName = name
        }
        userRepository.persist(user)
        Log.info("Provisioned new user for subject ${principal.subjectId}")
        return user
    }
}
