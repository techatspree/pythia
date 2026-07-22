package io.github.theestimator.service

import io.github.theestimator.i18n.SupportedLanguage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PreferredLanguageTest {

    @Test
    fun `english accept-language maps to EN`() {
        assertEquals(SupportedLanguage.EN, preferredLanguage("en"))
        assertEquals(SupportedLanguage.EN, preferredLanguage("EN"))
        assertEquals(SupportedLanguage.EN, preferredLanguage("  en  "))
        assertEquals(SupportedLanguage.EN, preferredLanguage("en-US,en;q=0.9,de;q=0.8"))
    }

    @Test
    fun `anything else maps to DE`() {
        assertEquals(SupportedLanguage.DE, preferredLanguage("de"))
        assertEquals(SupportedLanguage.DE, preferredLanguage("de-DE"))
        assertEquals(SupportedLanguage.DE, preferredLanguage("fr"))
        assertEquals(SupportedLanguage.DE, preferredLanguage(null))
        assertEquals(SupportedLanguage.DE, preferredLanguage(""))
    }
}
