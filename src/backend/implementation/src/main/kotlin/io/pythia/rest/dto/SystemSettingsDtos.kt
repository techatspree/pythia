package io.pythia.rest.dto

// Wire DTOs for the per-installation system settings (task-146).
//
// The stylesheet itself is NOT carried here — it is served as text/css from
// GET /api/system/css so the frontend can pull it in with a plain <link>.
// `hasCustomCss` only tells the admin UI whether there is one to remove.

data class SystemSettingsDto(
    val displayName: String?,
    val hasCustomCss: Boolean,
    val customCssFilename: String?,
    val customCssUpdatedAt: String?,
    /** Hours of a working day: divides accompanying work's hours into person-days (task-213). */
    val hoursPerDay: Double,
    /** Whether the read-only MCP interface at /api/mcp answers (task-218); off by default. */
    val mcpEnabled: Boolean
)

/**
 * Body of `PUT /api/system/mcp` (task-218). Its own endpoint for the same
 * reason as hours-per-day: a null `displayName` in `PUT /api/system` clears it.
 */
data class SystemSettingsMcpDto(
    val enabled: Boolean
)

/**
 * Body of `PUT /api/system/hours-per-day` (task-213). Its own endpoint, not a
 * field of `SystemSettingsUpdateDto`: there a null `displayName` means "clear
 * the name", so an hours-only PUT would wipe it.
 */
data class SystemSettingsHoursPerDayDto(
    val hoursPerDay: Double
)

data class SystemSettingsUpdateDto(
    val displayName: String? = null
)
