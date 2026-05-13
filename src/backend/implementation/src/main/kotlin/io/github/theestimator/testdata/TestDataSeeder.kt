package io.github.theestimator.testdata

import io.github.theestimator.domain.AdditionalCostType
import io.github.theestimator.domain.draft.*
import io.github.theestimator.repository.DraftEstimationVersionRepository
import io.github.theestimator.repository.ProjectRepository
import io.github.theestimator.service.EstimationService
import io.github.theestimator.service.EstimationVersionService
import io.github.theestimator.service.ProjectService
import io.quarkus.arc.profile.IfBuildProfile
import io.quarkus.runtime.StartupEvent
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional

@ApplicationScoped
@IfBuildProfile(anyOf = ["dev", "dev-local"])
class TestDataSeeder(
    private val projectService: ProjectService,
    private val estimationService: EstimationService,
    private val estimationVersionService: EstimationVersionService,
    private val draftRepository: DraftEstimationVersionRepository,
    private val projectRepository: ProjectRepository,
    private val entityManager: EntityManager
) {

    @Transactional
    fun seed(@Observes @Priority(1) event: StartupEvent) {
        if (projectRepository.count() > 0L) return
        seedWebshop()
        seedMobileApp()
    }

    private fun seedWebshop() {
        val project = projectService.create(
            name = "Webshop Redesign",
            description = "Komplette Neuentwicklung der E-Commerce-Plattform",
            client = "RetailCorp GmbH"
        )
        val estimation = estimationService.create(
            offer = "WS-2026-001",
            project = project,
            description = "Erstschätzung Webshop-Redesign"
        )

        val draft1 = DraftEstimationVersion().apply {
            this.estimation = estimation
            this.versionNumber = 1
        }

        draft1.parameters.addAll(listOf(
            DraftEstimationParameter().apply { name = "Tagessatz"; value = 900.0; version = draft1 },
            DraftEstimationParameter().apply { name = "Standardabweichungsfaktor"; value = 2.0; version = draft1 },
            DraftEstimationParameter().apply { name = "Vertriebszuschlag"; value = 0.12; version = draft1 }
        ))

        draft1.effortDrivers.add(DraftEffortDriver().apply {
            description = "Qualitätssicherung (QA)"
            factor = 0.15
            comment = "Inkl. automatisierter Tests"
            version = draft1
        })

        val phaseKO = DraftProjectPhase().apply { name = "Konzeption"; abbreviation = "KO"; durationWeeks = 3.0; version = draft1 }
        val phaseUM = DraftProjectPhase().apply { name = "Umsetzung"; abbreviation = "UM"; durationWeeks = 12.0; version = draft1 }
        val phaseAB = DraftProjectPhase().apply { name = "Abnahme"; abbreviation = "AB"; durationWeeks = 2.0; version = draft1 }
        draft1.phases.addAll(listOf(phaseKO, phaseUM, phaseAB))

        val groupU01 = DraftEstimationItemGroup().apply { title = "U01: Konzeption"; version = draft1 }
        groupU01.items.addAll(listOf(
            DraftFixedEstimationItem().apply { description = "Anforderungsworkshop & Kickoff"; minEffort = 1.0; expectedEffort = 2.0; maxEffort = 3.0; phase = phaseKO; group = groupU01 },
            DraftFixedEstimationItem().apply { description = "Systemarchitektur & Tech-Stack-Entscheidung"; minEffort = 2.0; expectedEffort = 3.0; maxEffort = 5.0; phase = phaseKO; group = groupU01 },
            DraftFixedEstimationItem().apply { description = "Datenbankdesign & ER-Modell"; minEffort = 1.0; expectedEffort = 2.0; maxEffort = 4.0; phase = phaseKO; group = groupU01 },
            DraftTimeRelativeEstimationItem().apply { description = "Projektbegleitung"; unit = "h/Woche"; minEffort = 2.0; expectedEffort = 4.0; maxEffort = 8.0; phase = phaseKO; group = groupU01 }
        ))

        val groupU02 = DraftEstimationItemGroup().apply { title = "U02: Frontend Redesign"; version = draft1 }
        groupU02.items.addAll(listOf(
            DraftFixedEstimationItem().apply { description = "Produktlisting & Suchfunktion"; minEffort = 3.0; expectedEffort = 5.0; maxEffort = 8.0; phase = phaseUM; group = groupU02 },
            DraftFixedEstimationItem().apply { description = "Warenkorb & Checkout-Prozess"; minEffort = 5.0; expectedEffort = 8.0; maxEffort = 12.0; phase = phaseUM; group = groupU02 },
            DraftFixedEstimationItem().apply { description = "Benutzerkonto & Login"; minEffort = 2.0; expectedEffort = 4.0; maxEffort = 6.0; phase = phaseUM; group = groupU02 },
            DraftFixedEstimationItem().apply { description = "Responsive Design & Mobile Optimierung"; minEffort = 2.0; expectedEffort = 3.0; maxEffort = 5.0; phase = phaseUM; group = groupU02 }
        ))

        val groupU03 = DraftEstimationItemGroup().apply { title = "U03: Backend & Datenbank"; version = draft1 }
        groupU03.items.addAll(listOf(
            DraftFixedEstimationItem().apply { description = "REST API Endpoints (CRUD)"; minEffort = 4.0; expectedEffort = 6.0; maxEffort = 9.0; phase = phaseUM; group = groupU03 },
            DraftFixedEstimationItem().apply { description = "Authentifizierung & Autorisierung"; minEffort = 2.0; expectedEffort = 4.0; maxEffort = 6.0; phase = phaseUM; group = groupU03 },
            DraftFixedEstimationItem().apply { description = "Datenbankmigrationen & Seeding"; minEffort = 1.0; expectedEffort = 2.0; maxEffort = 3.0; phase = phaseUM; group = groupU03 },
            DraftFixedEstimationItem().apply { description = "Payment-Integration (Stripe)"; minEffort = 3.0; expectedEffort = 5.0; maxEffort = 8.0; phase = phaseUM; group = groupU03 }
        ))

        val groupU04 = DraftEstimationItemGroup().apply { title = "U04: Abnahme & Go-live"; version = draft1 }
        groupU04.items.addAll(listOf(
            DraftFixedEstimationItem().apply { description = "Integrationstests & E2E-Tests"; minEffort = 2.0; expectedEffort = 3.0; maxEffort = 5.0; phase = phaseAB; group = groupU04 },
            DraftFixedEstimationItem().apply { description = "User Acceptance Testing (UAT)"; minEffort = 1.0; expectedEffort = 2.0; maxEffort = 3.0; phase = phaseAB; group = groupU04 },
            DraftFixedEstimationItem().apply { description = "Go-live, Deployment & Monitoring-Setup"; minEffort = 1.0; expectedEffort = 2.0; maxEffort = 3.0; phase = phaseAB; group = groupU04 }
        ))

        draft1.itemGroups.addAll(listOf(groupU01, groupU02, groupU03, groupU04))

        draft1.additionalCosts.addAll(listOf(
            DraftAdditionalCost().apply { description = "Software-Lizenzen (Figma, JIRA)"; amount = 3500.0; type = AdditionalCostType.ONE_TIME; phase = phaseKO; version = draft1 },
            DraftAdditionalCost().apply { description = "Hosting & Infrastruktur (AWS)"; amount = 0.0; type = AdditionalCostType.RECURRING; amountPerWeek = 250.0; phase = phaseUM; version = draft1 }
        ))

        draftRepository.persist(draft1)

        estimationVersionService.submitDraft(estimation.id!!)

        // Flush so draft1's DELETE is committed to DB before draft2's INSERT is queued.
        // Hibernate processes INSERTs before DELETEs within a flush; without this explicit
        // flush, INSERT(draft2) fires while draft1 still exists, violating the unique
        // constraint on draft_estimation_versions.estimation_id.
        entityManager.flush()

        val draft2 = DraftEstimationVersion().apply {
            this.estimation = estimation
            this.versionNumber = 2
            this.notes = "Scope nach Kunden-Feedback angepasst — Mobile-Optimierung ersetzt durch UX-Konzept"
        }

        draft2.parameters.addAll(listOf(
            DraftEstimationParameter().apply { name = "Tagessatz"; value = 900.0; version = draft2 },
            DraftEstimationParameter().apply { name = "Standardabweichungsfaktor"; value = 2.0; version = draft2 },
            DraftEstimationParameter().apply { name = "Vertriebszuschlag"; value = 0.12; version = draft2 }
        ))

        draft2.effortDrivers.addAll(listOf(
            DraftEffortDriver().apply { description = "Qualitätssicherung (QA)"; factor = 0.15; comment = "Inkl. automatisierter Tests"; version = draft2 },
            DraftEffortDriver().apply { description = "Technische Komplexität"; factor = 0.10; comment = "Legacy-System-Anbindung (SAP)"; version = draft2 }
        ))

        val phase2KO = DraftProjectPhase().apply { name = "Konzeption"; abbreviation = "KO"; durationWeeks = 4.0; version = draft2 }
        val phase2UM = DraftProjectPhase().apply { name = "Umsetzung"; abbreviation = "UM"; durationWeeks = 10.0; version = draft2 }
        val phase2AB = DraftProjectPhase().apply { name = "Abnahme"; abbreviation = "AB"; durationWeeks = 2.0; version = draft2 }
        draft2.phases.addAll(listOf(phase2KO, phase2UM, phase2AB))

        val group2U01 = DraftEstimationItemGroup().apply { title = "U01: Konzeption"; version = draft2 }
        group2U01.items.addAll(listOf(
            DraftFixedEstimationItem().apply { description = "Anforderungsworkshop & Kickoff"; minEffort = 1.0; expectedEffort = 2.0; maxEffort = 4.0; phase = phase2KO; group = group2U01 },
            DraftFixedEstimationItem().apply { description = "Systemarchitektur & Tech-Stack-Entscheidung"; minEffort = 3.0; expectedEffort = 4.0; maxEffort = 6.0; phase = phase2KO; group = group2U01 },
            DraftFixedEstimationItem().apply { description = "Datenbankdesign & ER-Modell"; minEffort = 1.0; expectedEffort = 2.0; maxEffort = 3.0; phase = phase2KO; group = group2U01 },
            DraftFixedEstimationItem().apply { description = "UX-Konzept & Wireframes"; minEffort = 3.0; expectedEffort = 5.0; maxEffort = 8.0; phase = phase2KO; group = group2U01 },
            DraftTimeRelativeEstimationItem().apply { description = "Projektbegleitung"; unit = "h/Woche"; minEffort = 2.0; expectedEffort = 4.0; maxEffort = 8.0; phase = phase2KO; group = group2U01 }
        ))

        val group2U02 = DraftEstimationItemGroup().apply { title = "U02: Frontend Redesign"; version = draft2 }
        group2U02.items.addAll(listOf(
            DraftFixedEstimationItem().apply { description = "Produktlisting & Suchfunktion"; minEffort = 3.0; expectedEffort = 5.0; maxEffort = 7.0; phase = phase2UM; group = group2U02 },
            DraftFixedEstimationItem().apply { description = "Warenkorb & Checkout-Prozess"; minEffort = 5.0; expectedEffort = 8.0; maxEffort = 13.0; phase = phase2UM; group = group2U02 },
            DraftFixedEstimationItem().apply { description = "Benutzerkonto & Login (OAuth2)"; minEffort = 2.0; expectedEffort = 4.0; maxEffort = 6.0; phase = phase2UM; group = group2U02 },
            DraftFixedEstimationItem().apply { description = "Produkt-Detailseite & Bildergalerie"; minEffort = 1.0; expectedEffort = 2.0; maxEffort = 4.0; phase = phase2UM; group = group2U02 }
        ))

        val group2U03 = DraftEstimationItemGroup().apply { title = "U03: Backend & Datenbank"; version = draft2 }
        group2U03.items.addAll(listOf(
            DraftFixedEstimationItem().apply { description = "REST API Endpoints (CRUD)"; minEffort = 4.0; expectedEffort = 6.0; maxEffort = 9.0; phase = phase2UM; group = group2U03 },
            DraftFixedEstimationItem().apply { description = "Authentifizierung & Autorisierung"; minEffort = 2.0; expectedEffort = 3.0; maxEffort = 5.0; phase = phase2UM; group = group2U03 },
            DraftFixedEstimationItem().apply { description = "Datenbankmigrationen & Seeding"; minEffort = 1.0; expectedEffort = 2.0; maxEffort = 3.0; phase = phase2UM; group = group2U03 },
            DraftFixedEstimationItem().apply { description = "Payment-Integration (Stripe)"; minEffort = 3.0; expectedEffort = 5.0; maxEffort = 8.0; phase = phase2UM; group = group2U03 },
            DraftFixedEstimationItem().apply { description = "E-Mail-Benachrichtigungen (Bestellung/Versand)"; minEffort = 1.0; expectedEffort = 2.0; maxEffort = 4.0; phase = phase2UM; group = group2U03 }
        ))

        val group2U04 = DraftEstimationItemGroup().apply { title = "U04: Abnahme & Go-live"; version = draft2 }
        group2U04.items.addAll(listOf(
            DraftFixedEstimationItem().apply { description = "Integrationstests & E2E-Tests"; minEffort = 2.0; expectedEffort = 3.0; maxEffort = 5.0; phase = phase2AB; group = group2U04 },
            DraftFixedEstimationItem().apply { description = "User Acceptance Testing (UAT)"; minEffort = 2.0; expectedEffort = 3.0; maxEffort = 4.0; phase = phase2AB; group = group2U04 },
            DraftFixedEstimationItem().apply { description = "Go-live, Deployment & Monitoring-Setup"; minEffort = 1.0; expectedEffort = 2.0; maxEffort = 3.0; phase = phase2AB; group = group2U04 }
        ))

        draft2.itemGroups.addAll(listOf(group2U01, group2U02, group2U03, group2U04))

        draft2.additionalCosts.addAll(listOf(
            DraftAdditionalCost().apply { description = "Software-Lizenzen (Figma, JIRA, Confluence)"; amount = 2500.0; type = AdditionalCostType.ONE_TIME; phase = phase2KO; version = draft2 },
            DraftAdditionalCost().apply { description = "Hosting & Infrastruktur (AWS)"; amount = 0.0; type = AdditionalCostType.RECURRING; amountPerWeek = 300.0; phase = phase2UM; version = draft2 }
        ))

        draftRepository.persist(draft2)
    }

    private fun seedMobileApp() {
        val project = projectService.create(
            name = "Mobile App MVP",
            description = "Native iOS/Android-App für Kunden-Self-Service",
            client = "FinanceAG"
        )
        val estimation = estimationService.create(
            offer = "MA-2026-001",
            project = project,
            description = "MVP-Umfangsschätzung"
        )

        val draft = DraftEstimationVersion().apply {
            this.estimation = estimation
            this.versionNumber = 1
        }

        draft.parameters.addAll(listOf(
            DraftEstimationParameter().apply { name = "Tagessatz"; value = 950.0; comment = "Mobile-Entwickler-Rate"; version = draft },
            DraftEstimationParameter().apply { name = "Standardabweichungsfaktor"; value = 2.0; version = draft },
            DraftEstimationParameter().apply { name = "Vertriebszuschlag"; value = 0.15; version = draft }
        ))

        draft.effortDrivers.add(DraftEffortDriver().apply {
            description = "iOS & Android Multiplattform"
            factor = 0.20
            comment = "Native Implementierung auf beiden Plattformen"
            version = draft
        })

        val phaseKD = DraftProjectPhase().apply { name = "Konzeption & Design"; abbreviation = "KD"; durationWeeks = 4.0; version = draft }
        val phaseS1 = DraftProjectPhase().apply { name = "Sprint 1 – Grundlagen"; abbreviation = "S1"; durationWeeks = 6.0; version = draft }
        val phaseS2 = DraftProjectPhase().apply { name = "Sprint 2 – Features"; abbreviation = "S2"; durationWeeks = 6.0; version = draft }
        val phaseAS = DraftProjectPhase().apply { name = "App Store Release"; abbreviation = "AS"; durationWeeks = 2.0; version = draft }
        draft.phases.addAll(listOf(phaseKD, phaseS1, phaseS2, phaseAS))

        val groupM01 = DraftEstimationItemGroup().apply { title = "M01: Konzeption & UX"; version = draft }
        groupM01.items.addAll(listOf(
            DraftFixedEstimationItem().apply { description = "UX Research & Nutzerinterviews"; minEffort = 2.0; expectedEffort = 3.0; maxEffort = 5.0; phase = phaseKD; group = groupM01 },
            DraftFixedEstimationItem().apply { description = "UI-Design & Designsystem"; minEffort = 5.0; expectedEffort = 8.0; maxEffort = 12.0; phase = phaseKD; group = groupM01 },
            DraftFixedEstimationItem().apply { description = "App-Architektur & Projektsetup"; minEffort = 2.0; expectedEffort = 3.0; maxEffort = 4.0; phase = phaseKD; group = groupM01 }
        ))

        val groupM02 = DraftEstimationItemGroup().apply { title = "M02: App Features"; version = draft }
        groupM02.items.addAll(listOf(
            DraftFixedEstimationItem().apply { description = "Authentifizierung (Biometrie, PIN)"; minEffort = 3.0; expectedEffort = 5.0; maxEffort = 8.0; phase = phaseS1; group = groupM02 },
            DraftFixedEstimationItem().apply { description = "Dashboard & Kontoübersicht"; minEffort = 3.0; expectedEffort = 5.0; maxEffort = 8.0; phase = phaseS1; group = groupM02 },
            DraftFixedEstimationItem().apply { description = "Push-Benachrichtigungen"; minEffort = 2.0; expectedEffort = 3.0; maxEffort = 5.0; phase = phaseS1; group = groupM02 },
            DraftFixedEstimationItem().apply { description = "Transaktionshistorie & Filter"; minEffort = 3.0; expectedEffort = 5.0; maxEffort = 7.0; phase = phaseS2; group = groupM02 },
            DraftFixedEstimationItem().apply { description = "Profil & Einstellungen"; minEffort = 2.0; expectedEffort = 3.0; maxEffort = 4.0; phase = phaseS2; group = groupM02 },
            DraftFixedEstimationItem().apply { description = "Offline-Modus & Datensynchronisation"; minEffort = 4.0; expectedEffort = 7.0; maxEffort = 10.0; phase = phaseS2; group = groupM02 }
        ))

        val groupM03 = DraftEstimationItemGroup().apply { title = "M03: Backend & API"; version = draft }
        groupM03.items.addAll(listOf(
            DraftFixedEstimationItem().apply { description = "REST API Design & Dokumentation"; minEffort = 2.0; expectedEffort = 3.0; maxEffort = 4.0; phase = phaseS1; group = groupM03 },
            DraftFixedEstimationItem().apply { description = "Auth & JWT-Token-Service"; minEffort = 2.0; expectedEffort = 3.0; maxEffort = 5.0; phase = phaseS1; group = groupM03 },
            DraftFixedEstimationItem().apply { description = "Daten-API & Business Logic"; minEffort = 4.0; expectedEffort = 6.0; maxEffort = 9.0; phase = phaseS2; group = groupM03 }
        ))

        val groupM04 = DraftEstimationItemGroup().apply { title = "M04: Release & QA"; version = draft }
        groupM04.items.addAll(listOf(
            DraftFixedEstimationItem().apply { description = "App Store Einreichung (iOS & Android)"; minEffort = 2.0; expectedEffort = 3.0; maxEffort = 5.0; phase = phaseAS; group = groupM04 },
            DraftFixedEstimationItem().apply { description = "Regression-Tests & Bugfixing"; minEffort = 3.0; expectedEffort = 4.0; maxEffort = 6.0; phase = phaseAS; group = groupM04 },
            DraftFixedEstimationItem().apply { description = "Beta-Test & Feedback-Implementierung"; minEffort = 2.0; expectedEffort = 3.0; maxEffort = 5.0; phase = phaseAS; group = groupM04 }
        ))

        draft.itemGroups.addAll(listOf(groupM01, groupM02, groupM03, groupM04))

        draft.additionalCosts.addAll(listOf(
            DraftAdditionalCost().apply { description = "Apple Developer Program"; amount = 99.0; type = AdditionalCostType.ONE_TIME; phase = phaseAS; version = draft },
            DraftAdditionalCost().apply { description = "Google Play Store Gebühr"; amount = 25.0; type = AdditionalCostType.ONE_TIME; phase = phaseAS; version = draft },
            DraftAdditionalCost().apply { description = "Backend-Hosting (Firebase)"; amount = 0.0; type = AdditionalCostType.RECURRING; amountPerWeek = 150.0; phase = phaseS1; version = draft }
        ))

        draftRepository.persist(draft)

        estimationVersionService.submitDraft(estimation.id!!)
    }
}
