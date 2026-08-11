package io.pythia.rest

import io.pythia.repository.DraftMutationLogRepository
import io.pythia.rest.dto.ConflictDetailsDto
import io.pythia.service.UndoConflictException
import io.quarkus.narayana.jta.QuarkusTransaction
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

// UndoConflictException -> 409 ConflictDetailsDto. The mapper runs AFTER the
// resource transaction has ended, so the exception's detached blockingEntry
// can't lazily load its user/draftVersion. Re-load it by id in a fresh
// transaction and read the fields while attached.
@Provider
@ApplicationScoped
class UndoConflictExceptionMapper(
    private val logRepository: DraftMutationLogRepository
) : ExceptionMapper<UndoConflictException> {

    override fun toResponse(exception: UndoConflictException): Response {
        val dto = QuarkusTransaction.requiringNew().call {
            val entry = logRepository.findById(exception.blockingEntry.id!!)!!
            ConflictDetailsDto(
                message = "This draft was changed by a newer edit; the undo/redo would clobber it.",
                blockingSequenceNumber = entry.sequenceNumber,
                blockingUserId = entry.user!!.id!!,
                blockingUserDisplayName = entry.user!!.displayName ?: "",
                blockingKind = entry.kind ?: "",
                blockingCreatedAt = entry.createdAt!!,
                currentDraftRevision = entry.draftVersion!!.revision
            )
        }
        return Response.status(Response.Status.CONFLICT).entity(dto).build()
    }
}
