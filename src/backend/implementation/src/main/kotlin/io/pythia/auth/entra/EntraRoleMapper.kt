package io.pythia.auth.entra

import io.pythia.auth.Role
import jakarta.json.JsonString

/**
 * Maps Entra app-role claim values to domain [Role]s.
 *
 * The `roles` claim reaches us in two shapes:
 *  - from [EntraAuthFilter], read straight off the MicroProfile `JsonWebToken`,
 *    it is a `jakarta.json.JsonArray` whose elements are [JsonString] (parsson
 *    `JsonStringImpl`) — NOT `java.lang.String`;
 *  - from [EntraSecurityIdentityAugmentor] it is the already-extracted
 *    `Set<String>` on the `SecurityIdentity`.
 *
 * Casting a `JsonString` to `String` throws `ClassCastException`, so this
 * accepts `Any?` and coerces each element to its raw string value defensively
 * (a `JsonString` yields its unquoted `.string`; anything else its `toString`).
 */
fun entraRolesToDomain(rawRoles: Any?): Set<Role> {
    val elements: Collection<*> = when (rawRoles) {
        is Collection<*> -> rawRoles
        null -> emptyList<Any?>()
        else -> listOf(rawRoles)
    }
    val out = mutableSetOf<Role>()
    for (element in elements) {
        val name = when (element) {
            is JsonString -> element.string
            null -> continue
            else -> element.toString()
        }
        when (name.uppercase()) {
            "VIEWER" -> out += Role.VIEWER
            "ESTIMATOR" -> out += Role.ESTIMATOR
            "ADMIN" -> out += Role.ADMIN
        }
    }
    return out
}
