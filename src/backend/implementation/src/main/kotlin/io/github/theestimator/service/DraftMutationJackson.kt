package io.github.theestimator.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.theestimator.rest.dto.DraftUpdateDto
import jakarta.enterprise.context.ApplicationScoped

// The persisted form of a mutation: the `kind` discriminator plus before/after
// snapshots as the Jackson-clean wire DTO (the raw domain types lose data
// through Jackson — see task-088). Serialised with the Quarkus-configured
// ObjectMapper (Kotlin module registered), so plain data classes round-trip.
data class StoredMutation(
    val kind: String,
    val before: DraftUpdateDto,
    val after: DraftUpdateDto
)

@ApplicationScoped
class DraftMutationJackson(private val objectMapper: ObjectMapper) {

    fun toJson(mutation: StoredMutation): String = objectMapper.writeValueAsString(mutation)

    fun fromJson(json: String): StoredMutation = objectMapper.readValue(json, StoredMutation::class.java)
}
