package io.pythia.rest

import io.pythia.auth.CurrentUserProvider
import io.pythia.domain.Project
import io.pythia.domain.ProjectStatus
import io.pythia.domain.User
import io.pythia.repository.ProjectRepository
import io.pythia.rest.dto.EstimationCreateDto
import io.pythia.rest.dto.EstimationSummaryDto
import io.pythia.rest.dto.ProjectCreateDto
import io.pythia.rest.dto.ProjectDetailDto
import io.pythia.rest.dto.ProjectOwnerUpdateDto
import io.pythia.rest.dto.ProjectSummaryDto
import io.pythia.rest.dto.ProjectUpdateDto
import io.pythia.rest.dto.toDetailDto
import io.pythia.rest.dto.toSummaryDto
import io.pythia.service.CurrentUserService
import io.pythia.service.EstimationService
import io.pythia.service.ProjectService
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.logging.Log
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.BadRequestException
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
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import java.util.UUID

@Path("/api/projects")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("VIEWER")
@Tag(name = "Projects", description = "Projects and their estimations")
class ProjectResource(
    private val projectService: ProjectService,
    private val projectRepository: ProjectRepository,
    private val estimationService: EstimationService,
    private val currentUserProvider: CurrentUserProvider,
    private val currentUserService: CurrentUserService,
    private val identity: SecurityIdentity
) {

    // The current user's row, resolved READ-ONLY (task-200): a read must never
    // provision a user — only mutating requests and GET /api/auth/me do. A user
    // without a row yet simply owns nothing.
    private fun currentUserRowOrNull(): User? =
        currentUserProvider.current?.let { currentUserService.findUser(it) }

    private fun isAdmin(): Boolean = identity.hasRole("ADMIN")

    private fun canChangeOwner(project: Project, me: User?): Boolean =
        ProjectService.mayChangeOwner(project, me, isAdmin())

    @GET
    @Operation(summary = "List projects, optionally filtered by status and/or to the current user's own (owner=me)")
    @APIResponse(
        responseCode = "200",
        description = "The projects",
        content = [Content(schema = Schema(type = SchemaType.ARRAY, implementation = ProjectSummaryDto::class))]
    )
    @APIResponse(responseCode = "400", description = "Unsupported owner filter (only 'me' is supported)")
    fun listProjects(
        @QueryParam("status") status: ProjectStatus?,
        @QueryParam("owner") owner: String?
    ): Response {
        if (owner != null && owner != "me") {
            throw BadRequestException("Unsupported owner filter '$owner' (only 'me' is supported)")
        }
        val me = currentUserRowOrNull()
        val projects = when {
            owner == "me" -> me?.let { projectService.findOwnedBy(it, status) } ?: emptyList()
            status != null -> projectService.findByStatus(status)
            else -> projectService.findAll()
        }
        return Response.ok(projects.map { it.toSummaryDto(canChangeOwner(it, me)) }).build()
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    @RolesAllowed("ESTIMATOR")
    @Operation(summary = "Create a new project")
    @APIResponse(
        responseCode = "201",
        description = "The created project",
        content = [Content(schema = Schema(implementation = ProjectSummaryDto::class))]
    )
    fun createProject(dto: ProjectCreateDto): Response {
        // The creator owns the project (task-200). A POST is mutating, so
        // provisioning the current user here is allowed.
        val creator = currentUserService.ensureUser(currentUserProvider.get())
        val project = projectService.create(dto.name, dto.description, dto.client, creator)
        Log.info("Created project ${project.id} owned by user ${creator.id}")
        return Response.status(Response.Status.CREATED)
            .entity(project.toSummaryDto(canChangeOwner(project, creator))).build()
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get a single project with its estimations")
    @APIResponse(
        responseCode = "200",
        description = "The project",
        content = [Content(schema = Schema(implementation = ProjectDetailDto::class))]
    )
    @APIResponse(responseCode = "404", description = "Project not found")
    fun getProject(@PathParam("id") id: UUID): Response {
        val project = projectRepository.findById(id)
            ?: throw NotFoundException("Project not found: $id")
        return Response.ok(project.toDetailDto(canChangeOwner(project, currentUserRowOrNull()))).build()
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    @RolesAllowed("ESTIMATOR")
    @Operation(summary = "Update a project's metadata")
    @APIResponse(
        responseCode = "200",
        description = "The updated project",
        content = [Content(schema = Schema(implementation = ProjectSummaryDto::class))]
    )
    @APIResponse(responseCode = "404", description = "Project not found")
    fun updateProject(@PathParam("id") id: UUID, dto: ProjectUpdateDto): Response {
        val project = projectRepository.findById(id)
            ?: throw NotFoundException("Project not found: $id")
        dto.name?.let { project.name = it }
        dto.description?.let { project.description = it }
        dto.client?.let { project.client = it }
        return Response.ok(project.toSummaryDto(canChangeOwner(project, currentUserRowOrNull()))).build()
    }

    @PUT
    @Path("/{id}/owner")
    @Consumes(MediaType.APPLICATION_JSON)
    // Transactional like the other writes here, so the response DTO (which reads
    // the lazy `estimations`) is built while the session is still open.
    @Transactional
    @RolesAllowed("ESTIMATOR", "ADMIN")
    @Operation(summary = "Transfer the project to another owner (current owner or ADMIN only)")
    @APIResponse(
        responseCode = "200",
        description = "The project with its new owner",
        content = [Content(schema = Schema(implementation = ProjectDetailDto::class))]
    )
    @APIResponse(responseCode = "400", description = "Unknown user")
    @APIResponse(responseCode = "403", description = "Neither the project's owner nor an administrator")
    @APIResponse(responseCode = "404", description = "Project not found")
    fun changeOwner(@PathParam("id") id: UUID, dto: ProjectOwnerUpdateDto): Response {
        val caller = currentUserService.ensureUser(currentUserProvider.get())
        val project = projectService.changeOwner(id, dto.ownerId, caller, isAdmin())
        return Response.ok(project.toDetailDto(canChangeOwner(project, caller))).build()
    }

    @POST
    @Path("/{id}/estimations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    @RolesAllowed("ESTIMATOR")
    @Operation(summary = "Create an estimation under a project")
    @APIResponse(
        responseCode = "201",
        description = "The created estimation",
        content = [Content(schema = Schema(implementation = EstimationSummaryDto::class))]
    )
    @APIResponse(responseCode = "404", description = "Project not found")
    fun createEstimation(@PathParam("id") id: UUID, dto: EstimationCreateDto): Response {
        val project = projectRepository.findById(id)
            ?: throw NotFoundException("Project not found: $id")
        val estimation = estimationService.create(dto.offer, project, dto.description, dto.method)
        Log.info("Created estimation ${estimation.id} with method ${dto.method}")
        return Response.status(Response.Status.CREATED).entity(estimation.toSummaryDto()).build()
    }

    @POST
    @Path("/{id}/archive")
    @Transactional
    @RolesAllowed("ESTIMATOR")
    @Operation(summary = "Archive a project")
    @APIResponse(
        responseCode = "200",
        description = "The archived project",
        content = [Content(schema = Schema(implementation = ProjectSummaryDto::class))]
    )
    @APIResponse(responseCode = "404", description = "Project not found")
    fun archiveProject(@PathParam("id") id: UUID): Response {
        val project = projectService.archive(id)
        return Response.ok(project.toSummaryDto(canChangeOwner(project, currentUserRowOrNull()))).build()
    }
}
