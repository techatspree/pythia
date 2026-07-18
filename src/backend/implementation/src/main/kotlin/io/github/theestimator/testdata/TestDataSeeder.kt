package io.github.theestimator.testdata

import io.github.theestimator.domain.AdditionalCostType
import io.github.theestimator.domain.Estimation
import io.github.theestimator.domain.EstimationBucket
import io.github.theestimator.domain.draft.DraftAdditionalCost
import io.github.theestimator.domain.draft.DraftBucketedItemNode
import io.github.theestimator.domain.draft.DraftEffortDriver
import io.github.theestimator.domain.draft.DraftEstimationNode
import io.github.theestimator.domain.draft.DraftEstimationParameter
import io.github.theestimator.domain.draft.DraftEstimationVersion
import io.github.theestimator.domain.draft.DraftFixedItemNode
import io.github.theestimator.domain.draft.DraftGroupNode
import io.github.theestimator.domain.draft.DraftProjectPhase
import io.github.theestimator.domain.draft.DraftTimeRelativeItemNode
import io.github.theestimator.method.EstimationMethod
import io.github.theestimator.repository.DraftEstimationVersionRepository
import io.github.theestimator.repository.ProjectRepository
import io.github.theestimator.service.EstimationService
import io.github.theestimator.service.EstimationVersionService
import io.github.theestimator.service.ProjectService
import io.quarkus.arc.profile.IfBuildProfile
import io.quarkus.logging.Log
import io.quarkus.runtime.StartupEvent
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import java.util.UUID

// This dev-only seeder is a wall of illustrative demo data — effort estimates,
// prices, durations, week counts. Those literals ARE the data; extracting each
// into a named constant would obscure rather than clarify (MagicNumber). For the
// same reason the seed methods are intentionally long, flat fixture builders
// (LongMethod / CyclomaticComplexMethod), the leaf-builder helpers take the
// fields positionally (LongParameterList), and the per-scenario seed + builder
// methods add up (TooManyFunctions) — all suppressed for this fixture class.
@Suppress("MagicNumber", "LongMethod", "CyclomaticComplexMethod", "LongParameterList", "TooManyFunctions")
@ApplicationScoped
@IfBuildProfile("dev")
class TestDataSeeder(
    private val projectService: ProjectService,
    private val estimationService: EstimationService,
    private val estimationVersionService: EstimationVersionService,
    private val draftRepository: DraftEstimationVersionRepository,
    private val projectRepository: ProjectRepository,
    private val entityManager: EntityManager
) {

    // `event` is the CDI observed event — required by @Observes for the
    // observer signature even though the body does not read it.
    @Suppress("UnusedParameter")
    @Transactional
    fun seed(@Observes @Priority(1) event: StartupEvent) {
        if (projectRepository.count() > 0L) return
        seedWebshop()
        seedMobileApp()
        seedDataPlatform()
    }

    private fun fixedLeaf(
        version: DraftEstimationVersion,
        description: String,
        min: Double,
        exp: Double,
        max: Double,
        phase: DraftProjectPhase?
    ) = DraftFixedItemNode().apply {
        this.version = version
        this.description = description
        this.minEffort = min
        this.expectedEffort = exp
        this.maxEffort = max
        this.phase = phase
    }

    private fun timeRelativeLeaf(
        version: DraftEstimationVersion,
        description: String,
        unit: String,
        min: Double,
        exp: Double,
        max: Double,
        phase: DraftProjectPhase?
    ) = DraftTimeRelativeItemNode().apply {
        this.version = version
        this.description = description
        this.unit = unit
        this.minEffort = min
        this.expectedEffort = exp
        this.maxEffort = max
        this.phase = phase
    }

    private fun group(
        version: DraftEstimationVersion,
        title: String,
        children: List<DraftEstimationNode>
    ): DraftGroupNode {
        val groupNode = DraftGroupNode().apply {
            this.version = version
            this.title = title
        }
        children.forEachIndexed { idx, child ->
            child.parent = groupNode
            child.position = idx
        }
        groupNode.children.addAll(children)
        return groupNode
    }

    private fun addRoots(version: DraftEstimationVersion, roots: List<DraftGroupNode>) {
        roots.forEachIndexed { idx, root -> root.position = idx }
        version.roots.addAll(roots)
    }

    private fun bucket(estimation: Estimation, position: Int, label: String) =
        EstimationBucket().apply {
            this.id = UUID.randomUUID()
            this.estimation = estimation
            this.position = position
            this.label = label
        }

    private fun bucketedLeaf(
        version: DraftEstimationVersion,
        description: String,
        bucket: EstimationBucket,
        isSample: Boolean,
        min: Double? = null,
        exp: Double? = null,
        max: Double? = null
    ) = DraftBucketedItemNode().apply {
        this.version = version
        this.description = description
        this.bucket = bucket
        this.isSample = isSample
        this.minEffort = min
        this.expectedEffort = exp
        this.maxEffort = max
    }

    // Root-level leaves (no group wrapper): for the bucket + sampled method the
    // buckets are the grouping, matching how the task-104 editor renders.
    private fun addRootLeaves(version: DraftEstimationVersion, leaves: List<DraftEstimationNode>) {
        leaves.forEachIndexed { idx, leaf -> leaf.position = idx }
        version.roots.addAll(leaves)
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

        draft1.parameters.addAll(
            listOf(
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

        val phaseKO = DraftProjectPhase().apply {
            name = "Konzeption"
            abbreviation = "KO"
            durationWeeks = 3.0
            version = draft1
        }
        val phaseUM = DraftProjectPhase().apply {
            name = "Umsetzung"
            abbreviation = "UM"
            durationWeeks = 12.0
            version = draft1
        }
        val phaseAB = DraftProjectPhase().apply {
            name = "Abnahme"
            abbreviation = "AB"
            durationWeeks = 2.0
            version = draft1
        }
        draft1.phases.addAll(listOf(phaseKO, phaseUM, phaseAB))

        addRoots(
            draft1, listOf(
                group(
                    draft1, "U01: Konzeption", listOf(
                        fixedLeaf(draft1, "Anforderungsworkshop & Kickoff", 1.0, 2.0, 3.0, phaseKO),
                        fixedLeaf(draft1, "Systemarchitektur & Tech-Stack-Entscheidung", 2.0, 3.0, 5.0, phaseKO),
                        fixedLeaf(draft1, "Datenbankdesign & ER-Modell", 1.0, 2.0, 4.0, phaseKO),
                        timeRelativeLeaf(draft1, "Projektbegleitung", "h/Woche", 2.0, 4.0, 8.0, phaseKO)
                    )
                ),
                group(
                    draft1, "U02: Frontend Redesign", listOf(
                        fixedLeaf(draft1, "Produktlisting & Suchfunktion", 3.0, 5.0, 8.0, phaseUM),
                        fixedLeaf(draft1, "Warenkorb & Checkout-Prozess", 5.0, 8.0, 12.0, phaseUM),
                        fixedLeaf(draft1, "Benutzerkonto & Login", 2.0, 4.0, 6.0, phaseUM),
                        fixedLeaf(draft1, "Responsive Design & Mobile Optimierung", 2.0, 3.0, 5.0, phaseUM),
                        timeRelativeLeaf(draft1, "UX-Begleitung", "h/Woche", 4.0, 5.0, 16.0, phaseKO)
                    )
                ),
                group(
                    draft1, "U03: Backend & Datenbank", listOf(
                        fixedLeaf(draft1, "REST API Endpoints (CRUD)", 4.0, 6.0, 9.0, phaseUM),
                        group(
                            draft1, "Authentifizierung", listOf(
                                fixedLeaf(draft1, "Login & Session-Verwaltung", 2.0, 3.0, 5.0, phaseUM),
                                fixedLeaf(draft1, "OAuth2-Anbindung (Google, GitHub)", 2.0, 3.0, 5.0, phaseUM)
                            )
                        ),
                        fixedLeaf(draft1, "Datenbankmigrationen & Seeding", 1.0, 2.0, 3.0, phaseUM),
                        fixedLeaf(draft1, "Payment-Integration (Stripe)", 3.0, 5.0, 8.0, phaseUM)
                    )
                ),
                group(
                    draft1, "U04: Abnahme & Go-live", listOf(
                        fixedLeaf(draft1, "Integrationstests & E2E-Tests", 2.0, 3.0, 5.0, phaseAB),
                        fixedLeaf(draft1, "User Acceptance Testing (UAT)", 1.0, 2.0, 3.0, phaseAB),
                        fixedLeaf(draft1, "Go-live, Deployment & Monitoring-Setup", 1.0, 2.0, 3.0, phaseAB)
                    )
                )
            )
        )

        draft1.additionalCosts.addAll(
            listOf(
            DraftAdditionalCost().apply {
                description = "Software-Lizenzen (Figma, JIRA)"
                amount = 3500.0
                type = AdditionalCostType.ONE_TIME
                phase = phaseKO
                version = draft1
            },
            DraftAdditionalCost().apply {
                description = "Hosting & Infrastruktur (AWS)"
                amount = 0.0
                type = AdditionalCostType.RECURRING
                amountPerWeek = 250.0
                phase = phaseUM
                version = draft1
            }
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

        draft2.parameters.addAll(
            listOf(
            DraftEstimationParameter().apply { name = "Tagessatz"; value = 900.0; version = draft2 },
            DraftEstimationParameter().apply { name = "Standardabweichungsfaktor"; value = 2.0; version = draft2 },
            DraftEstimationParameter().apply { name = "Vertriebszuschlag"; value = 0.12; version = draft2 }
        ))

        draft2.effortDrivers.addAll(
            listOf(
            DraftEffortDriver().apply {
                description = "Qualitätssicherung (QA)"
                factor = 0.15
                comment = "Inkl. automatisierter Tests"
                version = draft2
            },
            DraftEffortDriver().apply {
                description = "Technische Komplexität"
                factor = 0.10
                comment = "Legacy-System-Anbindung (SAP)"
                version = draft2
            }
        ))

        val phase2KO = DraftProjectPhase().apply {
            name = "Konzeption"
            abbreviation = "KO"
            durationWeeks = 4.0
            version = draft2
        }
        val phase2UM = DraftProjectPhase().apply {
            name = "Umsetzung"
            abbreviation = "UM"
            durationWeeks = 10.0
            version = draft2
        }
        val phase2AB = DraftProjectPhase().apply {
            name = "Abnahme"
            abbreviation = "AB"
            durationWeeks = 2.0
            version = draft2
        }
        draft2.phases.addAll(listOf(phase2KO, phase2UM, phase2AB))

        addRoots(
            draft2, listOf(
                group(
                    draft2, "U01: Konzeption", listOf(
                        fixedLeaf(draft2, "Anforderungsworkshop & Kickoff", 1.0, 2.0, 4.0, phase2KO),
                        fixedLeaf(draft2, "Systemarchitektur & Tech-Stack-Entscheidung", 3.0, 4.0, 6.0, phase2KO),
                        fixedLeaf(draft2, "Datenbankdesign & ER-Modell", 1.0, 2.0, 3.0, phase2KO),
                        fixedLeaf(draft2, "UX-Konzept & Wireframes", 3.0, 5.0, 8.0, phase2KO),
                        timeRelativeLeaf(draft2, "Projektbegleitung", "h/Woche", 2.0, 4.0, 8.0, phase2KO)
                    )
                ),
                group(
                    draft2, "U02: Frontend Redesign", listOf(
                        fixedLeaf(draft2, "Produktlisting & Suchfunktion", 3.0, 5.0, 7.0, phase2UM),
                        fixedLeaf(draft2, "Warenkorb & Checkout-Prozess", 5.0, 8.0, 13.0, phase2UM),
                        fixedLeaf(draft2, "Benutzerkonto & Login (OAuth2)", 2.0, 4.0, 6.0, phase2UM),
                        fixedLeaf(draft2, "Produkt-Detailseite & Bildergalerie", 1.0, 2.0, 4.0, phase2UM)
                    )
                ),
                group(
                    draft2, "U03: Backend & Datenbank", listOf(
                        fixedLeaf(draft2, "REST API Endpoints (CRUD)", 4.0, 6.0, 9.0, phase2UM),
                        fixedLeaf(draft2, "Authentifizierung & Autorisierung", 2.0, 3.0, 5.0, phase2UM),
                        fixedLeaf(draft2, "Datenbankmigrationen & Seeding", 1.0, 2.0, 3.0, phase2UM),
                        group(
                            draft2, "Bezahlung", listOf(
                                fixedLeaf(draft2, "Stripe-Integration", 2.0, 4.0, 6.0, phase2UM),
                                fixedLeaf(draft2, "Sepa-Lastschrift", 1.0, 2.0, 4.0, phase2UM)
                            )
                        ),
                        fixedLeaf(draft2, "E-Mail-Benachrichtigungen (Bestellung/Versand)", 1.0, 2.0, 4.0, phase2UM)
                    )
                ),
                group(
                    draft2, "U04: Abnahme & Go-live", listOf(
                        fixedLeaf(draft2, "Integrationstests & E2E-Tests", 2.0, 3.0, 5.0, phase2AB),
                        fixedLeaf(draft2, "User Acceptance Testing (UAT)", 2.0, 3.0, 4.0, phase2AB),
                        fixedLeaf(draft2, "Go-live, Deployment & Monitoring-Setup", 1.0, 2.0, 3.0, phase2AB)
                    )
                )
            )
        )

        draft2.additionalCosts.addAll(
            listOf(
            DraftAdditionalCost().apply {
                description = "Software-Lizenzen (Figma, JIRA, Confluence)"
                amount = 2500.0
                type = AdditionalCostType.ONE_TIME
                phase = phase2KO
                version = draft2
            },
            DraftAdditionalCost().apply {
                description = "Hosting & Infrastruktur (AWS)"
                amount = 0.0
                type = AdditionalCostType.RECURRING
                amountPerWeek = 300.0
                phase = phase2UM
                version = draft2
            }
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

        draft.parameters.addAll(
            listOf(
            DraftEstimationParameter().apply {
                name = "Tagessatz"
                value = 950.0
                comment = "Mobile-Entwickler-Rate"
                version = draft
            },
            DraftEstimationParameter().apply { name = "Standardabweichungsfaktor"; value = 2.0; version = draft },
            DraftEstimationParameter().apply { name = "Vertriebszuschlag"; value = 0.15; version = draft }
        ))

        draft.effortDrivers.add(DraftEffortDriver().apply {
            description = "iOS & Android Multiplattform"
            factor = 0.20
            comment = "Native Implementierung auf beiden Plattformen"
            version = draft
        })

        val phaseKD = DraftProjectPhase().apply {
            name = "Konzeption & Design"
            abbreviation = "KD"
            durationWeeks = 4.0
            version = draft
        }
        val phaseS1 = DraftProjectPhase().apply {
            name = "Sprint 1 – Grundlagen"
            abbreviation = "S1"
            durationWeeks = 6.0
            version = draft
        }
        val phaseS2 = DraftProjectPhase().apply {
            name = "Sprint 2 – Features"
            abbreviation = "S2"
            durationWeeks = 6.0
            version = draft
        }
        val phaseAS = DraftProjectPhase().apply {
            name = "App Store Release"
            abbreviation = "AS"
            durationWeeks = 2.0
            version = draft
        }
        draft.phases.addAll(listOf(phaseKD, phaseS1, phaseS2, phaseAS))

        addRoots(
            draft, listOf(
                group(
                    draft, "M01: Konzeption & UX", listOf(
                        fixedLeaf(draft, "UX Research & Nutzerinterviews", 2.0, 3.0, 5.0, phaseKD),
                        fixedLeaf(draft, "UI-Design & Designsystem", 5.0, 8.0, 12.0, phaseKD),
                        fixedLeaf(draft, "App-Architektur & Projektsetup", 2.0, 3.0, 4.0, phaseKD)
                    )
                ),
                group(
                    draft, "M02: App Features", listOf(
                        fixedLeaf(draft, "Authentifizierung (Biometrie, PIN)", 3.0, 5.0, 8.0, phaseS1),
                        fixedLeaf(draft, "Dashboard & Kontoübersicht", 3.0, 5.0, 8.0, phaseS1),
                        fixedLeaf(draft, "Push-Benachrichtigungen", 2.0, 3.0, 5.0, phaseS1),
                        fixedLeaf(draft, "Transaktionshistorie & Filter", 3.0, 5.0, 7.0, phaseS2),
                        fixedLeaf(draft, "Profil & Einstellungen", 2.0, 3.0, 4.0, phaseS2),
                        group(
                            draft, "Offline-Modus", listOf(
                                fixedLeaf(draft, "Datensynchronisation", 3.0, 5.0, 8.0, phaseS2),
                                fixedLeaf(draft, "Konfliktauflösung", 2.0, 3.0, 5.0, phaseS2)
                            )
                        )
                    )
                ),
                group(
                    draft, "M03: Backend & API", listOf(
                        fixedLeaf(draft, "REST API Design & Dokumentation", 2.0, 3.0, 4.0, phaseS1),
                        fixedLeaf(draft, "Auth & JWT-Token-Service", 2.0, 3.0, 5.0, phaseS1),
                        fixedLeaf(draft, "Daten-API & Business Logic", 4.0, 6.0, 9.0, phaseS2)
                    )
                ),
                group(
                    draft, "M04: Release & QA", listOf(
                        fixedLeaf(draft, "App Store Einreichung (iOS & Android)", 2.0, 3.0, 5.0, phaseAS),
                        fixedLeaf(draft, "Regression-Tests & Bugfixing", 3.0, 4.0, 6.0, phaseAS),
                        fixedLeaf(draft, "Beta-Test & Feedback-Implementierung", 2.0, 3.0, 5.0, phaseAS)
                    )
                )
            )
        )

        addMobileAppCosts(draft, phaseS1, phaseAS)

        draftRepository.persist(draft)

        estimationVersionService.submitDraft(estimation.id!!)
    }

    private fun addMobileAppCosts(
        draft: DraftEstimationVersion,
        phaseS1: DraftProjectPhase,
        phaseAS: DraftProjectPhase
    ) {
        draft.additionalCosts.addAll(
            listOf(
            DraftAdditionalCost().apply {
                description = "Apple Developer Program"
                amount = 99.0
                type = AdditionalCostType.ONE_TIME
                phase = phaseAS
                version = draft
            },
            DraftAdditionalCost().apply {
                description = "Google Play Store Gebühr"
                amount = 25.0
                type = AdditionalCostType.ONE_TIME
                phase = phaseAS
                version = draft
            },
            DraftAdditionalCost().apply {
                description = "Backend-Hosting (Firebase)"
                amount = 0.0
                type = AdditionalCostType.RECURRING
                amountPerWeek = 150.0
                phase = phaseS1
                version = draft
            }
        ))
    }

    private fun seedDataPlatform() {
        val project = projectService.create(
            name = "Data Platform Migration",
            description = "Migration der Datenplattform auf ein Lakehouse",
            client = "DataWorks AG"
        )
        val estimation = estimationService.create(
            offer = "DP-2026-001",
            project = project,
            description = "Bucket-Schätzung Datenplattform",
            method = EstimationMethod.BUCKET_SAMPLED_PERT
        )

        val bucketS = bucket(estimation, 0, "S")
        val bucketM = bucket(estimation, 1, "M")
        val bucketL = bucket(estimation, 2, "L")
        estimation.buckets.addAll(listOf(bucketS, bucketM, bucketL))

        val draft = DraftEstimationVersion().apply {
            this.estimation = estimation
            this.versionNumber = 1
        }

        draft.parameters.addAll(
            listOf(
            DraftEstimationParameter().apply { name = "Tagessatz"; value = 950.0; version = draft },
            DraftEstimationParameter().apply { name = "Standardabweichungsfaktor"; value = 2.0; version = draft },
            DraftEstimationParameter().apply { name = "Vertriebszuschlag"; value = 0.12; version = draft }
        ))

        // Each bucket: one sample (three-point) + one non-sample that inherits the
        // bucket's sample mean via EstimationVersion.calculate().
        addRootLeaves(
            draft, listOf(
                bucketedLeaf(draft, "Ingest connector A", bucketS, isSample = true, min = 1.0, exp = 2.0, max = 3.0),
                bucketedLeaf(draft, "Ingest connector B", bucketS, isSample = false),
                bucketedLeaf(draft, "Transform pipeline X", bucketM, isSample = true, min = 3.0, exp = 5.0, max = 8.0),
                bucketedLeaf(draft, "Transform pipeline Y", bucketM, isSample = false),
                bucketedLeaf(draft, "Reporting dashboard", bucketL, isSample = true, min = 5.0, exp = 8.0, max = 13.0),
                bucketedLeaf(draft, "Data catalog", bucketL, isSample = false)
            )
        )

        draftRepository.persist(draft)

        Log.info(
            "Seeded bucket+sampled estimation ${estimation.id} " +
                "(method=${estimation.method}, buckets=${estimation.buckets.size})"
        )
    }
}
