package io.pythia.repository

import io.pythia.domain.Project
import io.pythia.domain.ProjectStatus
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class ProjectRepository : PanacheRepositoryBase<Project, UUID> {

    fun findByStatus(status: ProjectStatus): List<Project> =
        list("status", status)
}
