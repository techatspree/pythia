package io.github.theestimator.service

import io.github.theestimator.auth.CurrentUser
import io.github.theestimator.domain.User
import io.github.theestimator.i18n.SupportedLanguage
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

    // requestedLanguage seeds the language preference ONLY when the user is
    // created (first sighting); it never overwrites an existing user's stored
    // choice. Callers derive it from Accept-Language (see preferredLanguage).
    @Transactional
    fun ensureUser(principal: CurrentUser, requestedLanguage: SupportedLanguage? = null): User {
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
            requestedLanguage?.let { language = it.code }
        }
        userRepository.persist(user)
        Log.info("Provisioned new user for subject ${principal.subjectId} with language ${user.language}")
        return user
    }

    // Persist an explicit language choice for the current user (find-or-create,
    // then set). Explicit choice always wins over the seeded default.
    @Transactional
    fun updateLanguage(principal: CurrentUser, language: SupportedLanguage): User {
        val user = ensureUser(principal)
        user.language = language.code
        Log.info("Updated language for user ${user.id} to ${language.code}")
        return user
    }
}

// Maps an HTTP Accept-Language header to a SupportedLanguage: 'en' → EN,
// anything else (including a missing header) → DE. Used to seed a user's
// preference on first sighting.
fun preferredLanguage(acceptLanguageHeader: String?): SupportedLanguage =
    if (acceptLanguageHeader?.trim()?.lowercase()?.startsWith("en") == true) {
        SupportedLanguage.EN
    } else {
        SupportedLanguage.DE
    }
