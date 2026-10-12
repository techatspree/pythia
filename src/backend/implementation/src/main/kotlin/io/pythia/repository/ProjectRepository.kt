package io.pythia.repository

import io.pythia.domain.Project
import io.pythia.domain.ProjectStatus
import io.pythia.domain.User
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class ProjectRepository : PanacheRepositoryBase<Project, UUID> {

    fun findByStatus(status: ProjectStatus): List<Project> =
        list("status", status)

    // Projects owned by `owner`, optionally narrowed to one status (task-200).
    fun findByOwner(owner: User, status: ProjectStatus?): List<Project> =
        if (status == null) {
            list("owner", owner)
        } else {
            list("owner = ?1 and status = ?2", owner, status)
        }
}
