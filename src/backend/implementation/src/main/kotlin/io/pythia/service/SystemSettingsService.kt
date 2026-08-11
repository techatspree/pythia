package io.pythia.service

import io.pythia.domain.system.SystemEffortDriver
import io.pythia.domain.system.SystemSettings
import io.pythia.repository.SystemEffortDriverRepository
import io.pythia.repository.SystemSettingsRepository
import io.pythia.rest.dto.EffortDriverDto
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import java.time.Instant

// Per-installation settings (task-146). This is installation CONFIGURATION, not
// business logic — nothing here belongs in the KMP domain.
@ApplicationScoped
class SystemSettingsService(
    private val settingsRepository: SystemSettingsRepository,
    private val driverRepository: SystemEffortDriverRepository
) {

    fun settings(): SystemSettings = settingsRepository.get()

    fun standardDrivers(): List<SystemEffortDriver> = driverRepository.findAllOrdered()

    @Transactional
    fun updateDisplayName(name: String?) {
        val normalised = name?.trim()?.takeIf { it.isNotEmpty() }
        settingsRepository.get().displayName = normalised
        Log.info("System display name set to ${normalised ?: "<none>"}")
    }

    // Clear-and-rebuild, matching the DraftUpdateApplier convention: `position`
    // comes from the list index, so the caller's order is the stored order.
    @Transactional
    fun replaceStandardDrivers(drivers: List<EffortDriverDto>) {
        driverRepository.deleteAll()
        driverRepository.flush()
        drivers.forEachIndexed { index, dto ->
            driverRepository.persist(SystemEffortDriver().apply {
                position = index
                description = dto.description
                factor = dto.factor
                comment = dto.comment ?: ""
            })
        }
        Log.info("Replaced standard effort drivers (n=${drivers.size})")
    }

    @Transactional
    fun storeCss(filename: String?, css: String) {
        if (css.isBlank()) {
            Log.warn("Rejected custom CSS upload: empty body")
            throw badRequest("The uploaded stylesheet is empty")
        }
        val bytes = css.toByteArray(Charsets.UTF_8).size
        if (bytes > MAX_CSS_BYTES) {
            Log.warn("Rejected custom CSS upload: $bytes bytes exceeds the $MAX_CSS_BYTES byte limit")
            throw badRequest("The stylesheet is larger than $MAX_CSS_KB KB")
        }
        settingsRepository.get().apply {
            customCss = css
            customCssFilename = filename
            customCssUpdatedAt = Instant.now()
        }
        Log.info("Stored custom CSS ($bytes bytes, filename=${filename ?: "<unnamed>"})")
    }

    @Transactional
    fun clearCss() {
        settingsRepository.get().apply {
            customCss = null
            customCssFilename = null
            customCssUpdatedAt = null
        }
        Log.info("Cleared custom CSS")
    }

    private fun badRequest(msg: String) = WebApplicationException(msg, Response.Status.BAD_REQUEST)

    companion object {
        const val MAX_CSS_KB = 256
        const val BYTES_PER_KB = 1024
        const val MAX_CSS_BYTES = MAX_CSS_KB * BYTES_PER_KB
    }
}
