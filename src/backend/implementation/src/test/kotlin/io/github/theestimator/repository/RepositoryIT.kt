package io.github.theestimator.repository

import io.github.theestimator.domain.*
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@QuarkusTest
@Transactional
class RepositoryIT {

    @Inject
    lateinit var projectRepository: ProjectRepository

    @Inject
    lateinit var estimationRepository: EstimationRepository

    @Inject
    lateinit var versionRepository: EstimationVersionRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Test
    fun `persist and find project`() {
        val project = Project().apply {
            name = "IT Project"
            client = "Test Client"
        }
        projectRepository.persist(project)

        assertNotNull(project.id)
        val found = projectRepository.findById(project.id!!)
        assertNotNull(found)
        assertEquals("IT Project", found!!.name)
    }

    @Test
    fun `findByStatus returns matching projects`() {
        val active = Project().apply { name = "Active"; status = ProjectStatus.ACTIVE }
        val archived = Project().apply { name = "Archived"; status = ProjectStatus.ARCHIVED }
        projectRepository.persist(active)
        projectRepository.persist(archived)

        val activeProjects = projectRepository.findByStatus(ProjectStatus.ACTIVE)
        assertTrue(activeProjects.any { it.name == "Active" })
        assertFalse(activeProjects.any { it.name == "Archived" })
    }

    @Test
    fun `persist and find estimation by project`() {
        val project = Project().apply { name = "Repo Test Project" }
        projectRepository.persist(project)

        val estimation = Estimation().apply {
            offer = "Test Offer"
            this.project = project
        }
        estimationRepository.persist(estimation)

        val found = estimationRepository.findByProjectId(project.id!!)
        assertEquals(1, found.size)
        assertEquals("Test Offer", found[0].offer)
    }

    @Test
    fun `findLatestByEstimationId returns highest version number`() {
        val project = Project().apply { name = "Version Test Project" }
        projectRepository.persist(project)

        val estimation = Estimation().apply {
            offer = "Version Test"
            this.project = project
        }
        estimationRepository.persist(estimation)

        val v1 = EstimationVersion().apply {
            this.estimation = estimation
            versionNumber = 1
            status = EstimationVersionStatus.SUBMITTED
        }
        val v2 = EstimationVersion().apply {
            this.estimation = estimation
            versionNumber = 2
            status = EstimationVersionStatus.DRAFT
        }
        versionRepository.persist(v1)
        versionRepository.persist(v2)

        val latest = versionRepository.findLatestByEstimationId(estimation.id!!)
        assertNotNull(latest)
        assertEquals(2, latest!!.versionNumber)
    }

    @Test
    fun `persist user with entra subject id`() {
        val user = User().apply {
            displayName = "Test User"
            entraSubjectId = "abc-123-def"
        }
        userRepository.persist(user)

        val found = userRepository.findByEntraSubjectId("abc-123-def")
        assertNotNull(found)
        assertEquals("Test User", found!!.displayName)
    }
}
