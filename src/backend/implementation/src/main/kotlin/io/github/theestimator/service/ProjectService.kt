package io.github.theestimator.service

import io.github.theestimator.domain.Project
import io.github.theestimator.domain.ProjectStatus
import io.github.theestimator.domain.User
import io.github.theestimator.repository.ProjectRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.util.UUID

@ApplicationScoped
class ProjectService(
    private val projectRepository: ProjectRepository
) {

    fun findById(id: UUID): Project? = projectRepository.findById(id)

    fun findByStatus(status: ProjectStatus): List<Project> = projectRepository.findByStatus(status)

    fun findAll(): List<Project> = projectRepository.listAll()

    @Transactional
    fun create(name: String, description: String? = null, client: String? = null, owner: User? = null): Project {
        val project = Project().apply {
            this.name = name
            this.description = description
            this.client = client
            this.owner = owner
        }
        projectRepository.persist(project)
        return project
    }

    @Transactional
    fun archive(projectId: UUID): Project {
        val project = projectRepository.findById(projectId)
            ?: throw IllegalArgumentException("Project not found: $projectId")
        project.status = ProjectStatus.ARCHIVED
        return project
    }

    @Transactional
    fun activate(projectId: UUID): Project {
        val project = projectRepository.findById(projectId)
            ?: throw IllegalArgumentException("Project not found: $projectId")
        project.status = ProjectStatus.ACTIVE
        return project
    }
}
