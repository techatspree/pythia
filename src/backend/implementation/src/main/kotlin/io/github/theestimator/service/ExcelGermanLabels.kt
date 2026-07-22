package io.github.theestimator.service

/**
 * German labels mirror the customer's Excel workbook schema — this is external
 * DATA, not the module's source language. The importer/exporter parse and write
 * the customer's German workbook, so these sheet names, column headers, and
 * section markers must stay German. Every surrounding identifier, comment, and
 * log message in this package is English (phase-16, task-122).
 */
object ExcelGermanLabels {

    /** Worksheet (tab) names, shared by importer and exporter. */
    object Sheets {
        const val PARAMETERS = "Parameter"
        const val EFFORT_DRIVERS = "Aufwandstreiber"
        const val PHASES = "Pakete"
        const val PROJECT_STRUCTURE_PLAN = "Projektstrukturplan"
        const val ADDITIONAL_COSTS = "Zusatzkosten"
    }

    /** Default unit written/read for time-relative leaves. */
    const val HOURS_PER_WEEK_UNIT = "h/Woche"

    /** Column headers of the Projektstrukturplan sheet (excludes the method's own input columns). */
    object ProjectStructure {
        const val DESCRIPTION = "Beschreibung"
        const val MEAN = "Mittelwert"
        const val MEAN_PER_GROUP = "Mittelwert pro Gruppe"
        const val VARIANCE = "Varianz"
        const val VARIANCE_PER_GROUP = "Varianz pro Gruppe"
        const val RISK_SURCHARGE_PT = "Zuschl. Risiko (PT)"
        const val DRIVER_SURCHARGE_PT = "Zuschl. Aufwandstreiber (PT)"
        const val OFFER_PT = "Angebots-PT"
        const val OFFER_PT_PER_GROUP = "Angebots-PT pro Gruppe"
        const val COST = "Kosten"
        const val OFFER_PRICE = "Angebots-preis"
        const val PACKAGE = "Paket"
        const val ASSUMPTIONS = "Annahmen, Abgrenzungen, Kommentare"
    }

    /** Vocabulary of the Zusatzkosten sheet — section titles and column headers. */
    object AdditionalCosts {
        const val ONE_TIME_SECTION = "Einmalige Kosten"
        const val RECURRING_SECTION = "Laufende Kosten"
        const val DESCRIPTION = "Beschreibung"
        const val AMOUNT = "Betrag"
        const val PACKAGE = "Paket"
        const val AMOUNT_PER_WEEK = "Betrag pro Woche"
        const val TOTAL_COST = "Gesamtkosten"
    }

    /** Vocabulary of the Pakete (phases) sheet — the "Summe" end marker plus column headers. */
    object Phases {
        const val TOTAL_MARKER = "Summe"
        const val NAME = "Name"
        const val ABBREVIATION = "Kürzel"
        const val DURATION_WEEKS = "Laufzeit\nWochen"
        const val EFFORT_PT = "Aufwand\nPT"
        const val DEVELOPMENT_COST = "Kosten Entwicklung"
        const val ADDITIONAL_COST_ONE_TIME = "Zusatzkosten\neinmalig"
        const val ADDITIONAL_COST_RECURRING = "Zusatzkosten\nlaufend"
        const val OFFER_PRICE_WITH_SALES_SURCHARGE = "Angebotspreis\nmit VT-Zuschlag"
    }

    /** Column headers of the Parameter sheet. */
    object Parameters {
        const val NAME = "Name"
        const val VALUE = "Wert"
        const val COMMENT = "Kommentar"
    }

    /** Vocabulary of the Aufwandstreiber sheet — the summary end marker plus column headers. */
    object EffortDrivers {
        const val SUMMARY_MARKER = "Zusammenfassung der Aufwandstreiber"
        const val DESCRIPTION = "Aufwandstreiber Beschreibung"
        const val FACTOR = "Faktor"
        const val ADDITIONAL_EFFORT = "Zusatzaufwand"
        const val COMMENT = "Kommentar"
    }
}
