package io.github.theestimator.rest.dto

// Wire DTOs for the per-installation system settings (task-146).
//
// The stylesheet itself is NOT carried here — it is served as text/css from
// GET /api/system/css so the frontend can pull it in with a plain <link>.
// `hasCustomCss` only tells the admin UI whether there is one to remove.

data class SystemSettingsDto(
    val displayName: String?,
    val hasCustomCss: Boolean,
    val customCssFilename: String?,
    val customCssUpdatedAt: String?
)

data class SystemSettingsUpdateDto(
    val displayName: String? = null
)
