package io.pythia.service

import io.pythia.domain.Project
import io.pythia.domain.ProjectStatus
import io.pythia.domain.User
import io.pythia.repository.ProjectRepository
import io.pythia.repository.UserRepository
import io.quarkus.logging.Log
import io.quarkus.security.ForbiddenException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import jakarta.transaction.Transactional
import java.util.UUID

@ApplicationScoped
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository
) {

    fun findById(id: UUID): Project? = projectRepository.findById(id)

    fun findByStatus(status: ProjectStatus): List<Project> = projectRepository.findByStatus(status)

    fun findAll(): List<Project> = projectRepository.listAll()

    fun findOwnedBy(owner: User, status: ProjectStatus?): List<Project> =
        projectRepository.findByOwner(owner, status)

    /**
     * Transfers [projectId] to the user [newOwnerId] (task-200). Only the current
     * owner or an ADMIN may do this; ownership is transferred, never removed. The
     * caller is a provisioned user: this runs on a mutating request, which the
     * UserProvisioningFilter has already provisioned.
     */
    @Transactional
    fun changeOwner(projectId: UUID, newOwnerId: UUID, caller: User, callerIsAdmin: Boolean): Project {
        val project = projectRepository.findById(projectId)
            ?: throw NotFoundException("Project not found: $projectId")
        val previousOwnerId = project.owner?.id
        requireMayChangeOwner(project, caller, callerIsAdmin)
        val newOwner = userRepository.findById(newOwnerId)
            ?: throw BadRequestException("Unknown user: $newOwnerId")
        project.owner = newOwner
        Log.info("Project $projectId owner changed from $previousOwnerId to ${newOwner.id} by user ${caller.id}")
        return project
    }

    private fun requireMayChangeOwner(project: Project, caller: User, callerIsAdmin: Boolean) {
        if (!mayChangeOwner(project, caller, callerIsAdmin)) {
            Log.warn("Refused owner change of project ${project.id} by user ${caller.id}: neither owner nor ADMIN")
            throw ForbiddenException("Only the project's owner or an administrator may change its owner")
        }
    }

    companion object {
        /** The ownership rule, shared by the transfer and the `canChangeOwner` DTO flag. */
        fun mayChangeOwner(project: Project, user: User?, userIsAdmin: Boolean): Boolean =
            userIsAdmin || (user != null && project.owner?.id == user.id)
    }

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
