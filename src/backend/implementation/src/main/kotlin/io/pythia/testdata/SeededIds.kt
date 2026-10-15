package io.pythia.testdata

import java.util.Random
import java.util.UUID

/**
 * The seed of the dev fixture's ids (task-212). Changing it is allowed, but it
 * changes every seeded `logicalId` and bucket id, so every id captured from an
 * earlier fixture (a screenshot, an export, a bug report) goes stale.
 */
const val FIXTURE_SEED = 42L

private const val UUID_BYTES = 16

/**
 * The one id source of [TestDataSeeder] (task-212): the same seed gives the same
 * sequence, so every fresh dev database gets the same `logicalId`s and bucket
 * ids. Without it every start drew fresh random UUIDs, and anything ordered or
 * shown by id differed between two otherwise identical stacks (task-181).
 *
 * Dev fixture only — never use it for production ids. It is a seeded
 * [Random], not a cryptographically secure generator, because only a seeded
 * generator is reproducible. The UUIDs come from [UUID.nameUUIDFromBytes], which sets the
 * version (3) and variant bits itself, so no bit masks are hand-written here.
 *
 * Create one per seeding run and pass it explicitly to every builder; never
 * keep it in a field, or a second run would continue the sequence instead of
 * repeating it.
 */
class SeededIds(seed: Long = FIXTURE_SEED) {
    private val random = Random(seed)

    /** How many ids have been drawn; logged by the seeder. */
    var drawn: Int = 0
        private set

    fun next(): UUID {
        val bytes = ByteArray(UUID_BYTES)
        random.nextBytes(bytes)
        drawn++
        return UUID.nameUUIDFromBytes(bytes)
    }
}
