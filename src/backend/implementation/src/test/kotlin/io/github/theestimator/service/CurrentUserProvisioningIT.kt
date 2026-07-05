package io.github.theestimator.service

import io.github.theestimator.auth.CurrentUser
import io.github.theestimator.repository.UserRepository
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.UUID

@QuarkusTest
@Transactional
class CurrentUserProvisioningIT {

    @Inject
    lateinit var currentUserService: CurrentUserService

    @Inject
    lateinit var userRepository: UserRepository

    private fun principal(subject: String, displayName: String?) =
        CurrentUser(
            subjectId = subject,
            email = "$subject@example.com",
            displayName = displayName,
            roles = emptySet(),
            providerName = "dev"
        )

    @Test
    fun `ensureUser creates, reuses, and refreshes the display name`() {
        val subject = "subj-" + UUID.randomUUID()

        val created = currentUserService.ensureUser(principal(subject, "Alice"))
        assertNotNull(created.id)
        assertEquals("Alice", created.displayName)
        assertNotNull(userRepository.findByEntraSubjectId(subject))

        val reused = currentUserService.ensureUser(principal(subject, "Alice"))
        assertEquals(created.id, reused.id)

        val renamed = currentUserService.ensureUser(principal(subject, "Alice B"))
        assertEquals(created.id, renamed.id)
        assertEquals("Alice B", renamed.displayName)
    }
}
