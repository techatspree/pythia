package io.github.theestimator.repository

import io.github.theestimator.domain.Project
import io.github.theestimator.domain.ProjectStatus
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class ProjectRepository : PanacheRepositoryBase<Project, UUID> {

    fun findByStatus(status: ProjectStatus): List<Project> =
        list("status", status)
}
