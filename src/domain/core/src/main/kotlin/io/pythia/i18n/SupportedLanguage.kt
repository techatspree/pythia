@file:OptIn(ExperimentalJsExport::class)

package io.pythia.i18n

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/**
 * The canonical set of UI languages the tool supports. This one enum name is
 * shared by the backend (validation + persistence via [code]) and the frontend
 * (task-124+ consume the exported name). `code` is the ISO 639-1 code used on
 * the wire and in the `users.language` column.
 */
@JsExport
enum class SupportedLanguage(val code: String) {
    DE("de"),
    EN("en")
}

/**
 * Resolves a wire/DB language [code] (e.g. `"de"`) to its [SupportedLanguage],
 * or `null` when the code is not supported. Kept as a top-level function (not a
 * companion) so it does not run into `@JsExport` companion-export limitations;
 * it is used only by the JVM backend.
 */
fun fromCode(code: String): SupportedLanguage? =
    SupportedLanguage.entries.firstOrNull { it.code == code }
