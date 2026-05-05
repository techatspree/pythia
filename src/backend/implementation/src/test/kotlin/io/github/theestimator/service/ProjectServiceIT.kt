package io.github.theestimator.service

import io.github.theestimator.domain.ProjectStatus
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@QuarkusTest
@Transactional
class ProjectServiceIT {

    @Inject
    lateinit var projectService: ProjectService

    @Test
    fun `create project persists and returns with id`() {
        val project = projectService.create("IT Project", "Description", "Client A")

        assertNotNull(project.id)
        assertEquals("IT Project", project.name)
        assertEquals("Description", project.description)
        assertEquals("Client A", project.client)
        assertEquals(ProjectStatus.ACTIVE, project.status)
        assertNotNull(project.createdAt)
    }

    @Test
    fun `findById returns persisted project`() {
        val created = projectService.create("Find Me")
        val found = projectService.findById(created.id!!)

        assertNotNull(found)
        assertEquals("Find Me", found!!.name)
    }

    @Test
    fun `findAll returns all projects`() {
        projectService.create("Project A")
        projectService.create("Project B")

        val all = projectService.findAll()
        assertTrue(all.size >= 2)
    }

    @Test
    fun `archive sets status to ARCHIVED`() {
        val project = projectService.create("To Archive")
        val archived = projectService.archive(project.id!!)

        assertEquals(ProjectStatus.ARCHIVED, archived.status)
    }

    @Test
    fun `activate sets status to ACTIVE`() {
        val project = projectService.create("To Activate")
        projectService.archive(project.id!!)
        val activated = projectService.activate(project.id!!)

        assertEquals(ProjectStatus.ACTIVE, activated.status)
    }

    @Test
    fun `findByStatus filters correctly`() {
        val active = projectService.create("Active Project")
        val toArchive = projectService.create("Archived Project")
        projectService.archive(toArchive.id!!)

        val activeProjects = projectService.findByStatus(ProjectStatus.ACTIVE)
        assertTrue(activeProjects.any { it.id == active.id })
        assertFalse(activeProjects.any { it.id == toArchive.id })
    }

    @Test
    fun `archive unknown project throws`() {
        assertThrows<IllegalArgumentException> {
            projectService.archive(java.util.UUID.randomUUID())
        }
    }
}
