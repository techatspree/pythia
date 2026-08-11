package io.pythia.rest

import io.pythia.rest.dto.MerlinStructureDiffDto
import io.pythia.service.MerlinStructureChangedException
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

// MerlinStructureChangedException -> 409 MerlinStructureDiffDto (task-133). The
// diff is plain data carried on the exception, so unlike the undo conflict
// mapper there is nothing to re-load from the database here.
@Provider
@ApplicationScoped
class MerlinStructureChangedExceptionMapper : ExceptionMapper<MerlinStructureChangedException> {

    override fun toResponse(exception: MerlinStructureChangedException): Response {
        val diff = exception.diff
        Log.info(
            "Merlin export refused: structure drifted " +
                "(missingInMerlin=${diff.missingInMerlin.size}, " +
                "missingInEstimation=${diff.missingInEstimation.size}, " +
                "reordered=${diff.reordered.size})"
        )
        val dto = MerlinStructureDiffDto(
            missingInMerlin = diff.missingInMerlin,
            missingInEstimation = diff.missingInEstimation,
            reordered = diff.reordered,
            inSync = false
        )
        // The export endpoint @Produces octet-stream (it returns a document);
        // pin JSON here or the diff would be rendered with toString().
        return Response.status(Response.Status.CONFLICT)
            .entity(dto)
            .type(MediaType.APPLICATION_JSON)
            .build()
    }
}
