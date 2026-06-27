package io.github.theestimator.rest

import io.github.theestimator.domain.ProjectStatus
import io.github.theestimator.repository.ProjectRepository
import io.github.theestimator.rest.dto.EstimationCreateDto
import io.github.theestimator.rest.dto.ProjectCreateDto
import io.github.theestimator.rest.dto.ProjectUpdateDto
import io.github.theestimator.rest.dto.toDetailDto
import io.github.theestimator.rest.dto.toSummaryDto
import io.github.theestimator.service.EstimationService
import io.github.theestimator.service.ProjectService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

@Path("/api/projects")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
class ProjectResource(
    private val projectService: ProjectService,
    private val projectRepository: ProjectRepository,
    private val estimationService: EstimationService
) {

    @GET
    fun listProjects(@QueryParam("status") status: ProjectStatus?): Response {
        val projects = if (status != null) {
            projectService.findByStatus(status)
        } else {
            projectService.findAll()
        }
        return Response.ok(projects.map { it.toSummaryDto() }).build()
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    fun createProject(dto: ProjectCreateDto): Response {
        val project = projectService.create(dto.name, dto.description, dto.client)
        return Response.status(Response.Status.CREATED).entity(project.toSummaryDto()).build()
    }

    @GET
    @Path("/{id}")
    fun getProject(@PathParam("id") id: UUID): Response {
        val project = projectRepository.findById(id)
            ?: throw NotFoundException("Project not found: $id")
        return Response.ok(project.toDetailDto()).build()
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    fun updateProject(@PathParam("id") id: UUID, dto: ProjectUpdateDto): Response {
        val project = projectRepository.findById(id)
            ?: throw NotFoundException("Project not found: $id")
        dto.name?.let { project.name = it }
        dto.description?.let { project.description = it }
        dto.client?.let { project.client = it }
        return Response.ok(project.toSummaryDto()).build()
    }

    @POST
    @Path("/{id}/estimations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    fun createEstimation(@PathParam("id") id: UUID, dto: EstimationCreateDto): Response {
        val project = projectRepository.findById(id)
            ?: throw NotFoundException("Project not found: $id")
        val estimation = estimationService.create(dto.offer, project, dto.description)
        return Response.status(Response.Status.CREATED).entity(estimation.toSummaryDto()).build()
    }

    @POST
    @Path("/{id}/archive")
    @Transactional
    fun archiveProject(@PathParam("id") id: UUID): Response {
        val project = projectService.archive(id)
        return Response.ok(project.toSummaryDto()).build()
    }
}
