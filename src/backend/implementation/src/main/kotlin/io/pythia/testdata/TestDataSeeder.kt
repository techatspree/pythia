package io.pythia.testdata

import io.pythia.domain.AdditionalCostType
import io.pythia.domain.Estimation
import io.pythia.domain.EstimationBucket
import io.pythia.domain.draft.DraftAdditionalCost
import io.pythia.domain.draft.DraftBucketedItemNode
import io.pythia.domain.draft.DraftEffortDriver
import io.pythia.domain.draft.DraftEstimationNode
import io.pythia.domain.draft.DraftEstimationVersion
import io.pythia.domain.draft.DraftFixedItemNode
import io.pythia.domain.draft.DraftGroupNode
import io.pythia.domain.draft.DraftScheduleDependency
import io.pythia.domain.draft.DraftProjectPhase
import io.pythia.domain.draft.DraftTimeRelativeItemNode
import io.pythia.method.EstimationMethod
import io.pythia.repository.DraftEstimationVersionRepository
import io.pythia.repository.ProjectRepository
import io.pythia.service.EstimationService
import io.pythia.service.EstimationVersionService
import io.pythia.service.ProjectService
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
/**
 * Dev-only fixture data (`%dev` profile), seeded once on startup when the
 * database is empty.
 *
 * SCHEDULE GRAPHS (task-173) — the shapes are deliberate and differ per
 * estimation, so the net-plan editor, the levelled makespan and the
 * critical-path column all have something to show on a fresh database:
 *
 *  - Webshop, BOTH versions — a DIAMOND: U01 fans out to U02 and U03, which
 *    fan back into U04. Two branches in parallel and a critical path through
 *    only one of them. `U05: Noch zu schätzen` is in NO edge: it is the
 *    deliberately-unestimated group the estimation-session picker's preselect
 *    demo relies on.
 *  - Mobile App MVP — a CHAIN, M01 → M02 → M03 → M04. Everything critical, the
 *    makespan is the sum: the opposite behaviour to the diamond.
 *  - Data platform (bucket+sampled) — NO graph. Its roots are bucketed leaves
 *    and the bucket editor draws no net plan, so it also keeps the empty-graph
 *    state represented in the fixture.
 *
 * Edges go on a draft BEFORE `submitDraft`, because `snapshotDraft` copies what
 * is on the draft at that moment; otherwise every submitted version would have
 * an empty graph. Do not "tidy away" these graphs — three features depend on
 * them for their demo.
 */
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
        // Each seeder reports the edges it added, because counting at the end
        // would UNDERCOUNT: `submitDraft` moves a draft's edges onto the
        // submitted version and deletes the draft, so most of them are no
        // longer reachable through `draftRepository`.
        //
        // Deliberately NOT tacked onto seedDataPlatform's own log: that
        // estimation is the one left without a graph.
        val edges = seedWebshop() + seedMobileApp() + seedDataPlatform()
        Log.info("Seeded dev fixture: schedule edges=$edges")
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

    /**
     * One finish-to-start edge, by `logicalId`.
     *
     * `DraftEstimationNode.logicalId` is a `UUID` while the edge columns are
     * `String`, hence the `toString()`.
     */
    private fun dependency(
        version: DraftEstimationVersion,
        from: DraftEstimationNode,
        to: DraftEstimationNode
    ) = DraftScheduleDependency().apply {
        this.version = version
        this.fromLogicalId = from.logicalId.toString()
        this.toLogicalId = to.logicalId.toString()
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

    private fun seedWebshop(): Int {
        val project = projectService.create(
            name = SeededProjects.WEBSHOP,
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

        draft1.dailyRate = 900.0
        draft1.stdDevFactor = 2.0
        draft1.salesSurcharge = 0.12

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

        val v1u01 = group(
            draft1, "U01: Konzeption", listOf(
                fixedLeaf(draft1, "Anforderungsworkshop & Kickoff", 1.0, 2.0, 3.0, phaseKO),
                fixedLeaf(draft1, "Systemarchitektur & Tech-Stack-Entscheidung", 2.0, 3.0, 5.0, phaseKO),
                fixedLeaf(draft1, "Datenbankdesign & ER-Modell", 1.0, 2.0, 4.0, phaseKO),
                timeRelativeLeaf(draft1, "Projektbegleitung", "h/Woche", 2.0, 4.0, 8.0, phaseKO)
            )
        )

        val v1u02 = group(
            draft1, "U02: Frontend Redesign", listOf(
                fixedLeaf(draft1, "Produktlisting & Suchfunktion", 3.0, 5.0, 8.0, phaseUM),
                fixedLeaf(draft1, "Warenkorb & Checkout-Prozess", 5.0, 8.0, 12.0, phaseUM),
                fixedLeaf(draft1, "Benutzerkonto & Login", 2.0, 4.0, 6.0, phaseUM),
                fixedLeaf(draft1, "Responsive Design & Mobile Optimierung", 2.0, 3.0, 5.0, phaseUM),
                timeRelativeLeaf(draft1, "UX-Begleitung", "h/Woche", 4.0, 5.0, 16.0, phaseKO)
            )
        )

        val v1u03 = group(
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
        )

        val v1u04 = group(
            draft1, "U04: Abnahme & Go-live", listOf(
                fixedLeaf(draft1, "Integrationstests & E2E-Tests", 2.0, 3.0, 5.0, phaseAB),
                fixedLeaf(draft1, "User Acceptance Testing (UAT)", 1.0, 2.0, 3.0, phaseAB),
                fixedLeaf(draft1, "Go-live, Deployment & Monitoring-Setup", 1.0, 2.0, 3.0, phaseAB)
            )
        )

        addRoots(draft1, listOf(v1u01, v1u02, v1u03, v1u04))

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

        // Three workers, so the two middle branches of the diamond below actually
        // run in parallel; at teamFte 1 levelling would serialise them and the
        // diamond would render indistinguishably from a chain.
        draft1.teamFte = 3.0
        // U01 fans out to U02 and U03, which fan back into U04. Added BEFORE the
        // submit: `snapshotDraft` copies whatever is on the draft at that moment,
        // so edges added afterwards would never reach the submitted version.
        val draft1Deps = listOf(
                dependency(draft1, v1u01, v1u02),
                dependency(draft1, v1u01, v1u03),
                dependency(draft1, v1u02, v1u04),
                dependency(draft1, v1u03, v1u04)
        )
        draft1.scheduleDependencies.addAll(draft1Deps)
        val draft1Edges = draft1Deps.size

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

        draft2.dailyRate = 900.0
        draft2.stdDevFactor = 2.0
        draft2.salesSurcharge = 0.12

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

        val v2u01 = group(
            draft2, "U01: Konzeption", listOf(
                fixedLeaf(draft2, "Anforderungsworkshop & Kickoff", 1.0, 2.0, 4.0, phase2KO),
                fixedLeaf(draft2, "Systemarchitektur & Tech-Stack-Entscheidung", 3.0, 4.0, 6.0, phase2KO),
                fixedLeaf(draft2, "Datenbankdesign & ER-Modell", 1.0, 2.0, 3.0, phase2KO),
                fixedLeaf(draft2, "UX-Konzept & Wireframes", 3.0, 5.0, 8.0, phase2KO),
                timeRelativeLeaf(draft2, "Projektbegleitung", "h/Woche", 2.0, 4.0, 8.0, phase2KO)
            )
        )

        val v2u02 = group(
            draft2, "U02: Frontend Redesign", listOf(
                fixedLeaf(draft2, "Produktlisting & Suchfunktion", 3.0, 5.0, 7.0, phase2UM),
                fixedLeaf(draft2, "Warenkorb & Checkout-Prozess", 5.0, 8.0, 13.0, phase2UM),
                fixedLeaf(draft2, "Benutzerkonto & Login (OAuth2)", 2.0, 4.0, 6.0, phase2UM),
                fixedLeaf(draft2, "Produkt-Detailseite & Bildergalerie", 1.0, 2.0, 4.0, phase2UM)
            )
        )

        val v2u03 = group(
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
        )

        val v2u04 = group(
            draft2, "U04: Abnahme & Go-live", listOf(
                fixedLeaf(draft2, "Integrationstests & E2E-Tests", 2.0, 3.0, 5.0, phase2AB),
                fixedLeaf(draft2, "User Acceptance Testing (UAT)", 2.0, 3.0, 4.0, phase2AB),
                fixedLeaf(draft2, "Go-live, Deployment & Monitoring-Setup", 1.0, 2.0, 3.0, phase2AB)
            )
        )

        val v2u05 = group(
            draft2, "U05: Noch zu schätzen", listOf(
                fixedLeaf(draft2, "Wunschlisten & Merkzettel", 0.0, 0.0, 0.0, phase2UM),
                fixedLeaf(draft2, "Produktbewertungen & Rezensionen", 0.0, 0.0, 0.0, phase2UM),
                fixedLeaf(draft2, "Gutschein- & Rabattcode-System", 0.0, 0.0, 0.0, phase2UM),
                fixedLeaf(draft2, "Mehrsprachigkeit (i18n)", 0.0, 0.0, 0.0, phase2KO)
            )
        )

        addRoots(draft2, listOf(v2u01, v2u02, v2u03, v2u04, v2u05))

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

        draft2.teamFte = 3.0
        // Same diamond on the current draft. U05 ("Noch zu schätzen") appears in
        // NO edge: it is the deliberately-unestimated fixture the estimation
        // session picker's preselect demo needs.
        val draft2Deps = listOf(
                dependency(draft2, v2u01, v2u02),
                dependency(draft2, v2u01, v2u03),
                dependency(draft2, v2u02, v2u04),
                dependency(draft2, v2u03, v2u04)
        )
        draft2.scheduleDependencies.addAll(draft2Deps)
        val draft2Edges = draft2Deps.size

        draftRepository.persist(draft2)

        return draft1Edges + draft2Edges
    }

    private fun seedMobileApp(): Int {
        val project = projectService.create(
            name = SeededProjects.MOBILE_APP,
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

        draft.dailyRate = 950.0
        draft.stdDevFactor = 2.0
        draft.salesSurcharge = 0.15

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

        val m01 = group(
            draft, "M01: Konzeption & UX", listOf(
                fixedLeaf(draft, "UX Research & Nutzerinterviews", 2.0, 3.0, 5.0, phaseKD),
                fixedLeaf(draft, "UI-Design & Designsystem", 5.0, 8.0, 12.0, phaseKD),
                fixedLeaf(draft, "App-Architektur & Projektsetup", 2.0, 3.0, 4.0, phaseKD)
            )
        )

        val m02 = group(
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
        )

        val m03 = group(
            draft, "M03: Backend & API", listOf(
                fixedLeaf(draft, "REST API Design & Dokumentation", 2.0, 3.0, 4.0, phaseS1),
                fixedLeaf(draft, "Auth & JWT-Token-Service", 2.0, 3.0, 5.0, phaseS1),
                fixedLeaf(draft, "Daten-API & Business Logic", 4.0, 6.0, 9.0, phaseS2)
            )
        )

        val m04 = group(
            draft, "M04: Release & QA", listOf(
                fixedLeaf(draft, "App Store Einreichung (iOS & Android)", 2.0, 3.0, 5.0, phaseAS),
                fixedLeaf(draft, "Regression-Tests & Bugfixing", 3.0, 4.0, 6.0, phaseAS),
                fixedLeaf(draft, "Beta-Test & Feedback-Implementierung", 2.0, 3.0, 5.0, phaseAS)
            )
        )

        addRoots(draft, listOf(m01, m02, m03, m04))

        addMobileAppCosts(draft, phaseS1, phaseAS)

        // Two workers. A CHAIN, not a second diamond: every task is critical and
        // the makespan is the sum, which is the opposite scheduler behaviour to
        // the webshop's diamond — so the fixture demonstrates both.
        draft.teamFte = 2.0
        val mobileDeps = listOf(
            dependency(draft, m01, m02),
            dependency(draft, m02, m03),
            dependency(draft, m03, m04)
        )
        draft.scheduleDependencies.addAll(mobileDeps)

        draftRepository.persist(draft)

        estimationVersionService.submitDraft(estimation.id!!)

        return mobileDeps.size
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

    private fun seedDataPlatform(): Int {
        val project = projectService.create(
            name = SeededProjects.DATA_PLATFORM,
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

        draft.dailyRate = 950.0
        draft.stdDevFactor = 2.0
        draft.salesSurcharge = 0.12

        // Each bucket: one sample (three-point) + one non-sample that inherits the
        // bucket's sample mean via EstimationVersion.calculate().
        //
        // The roots are deliberately NOT flat (task-150): a bucket estimation
        // legitimately contains GROUP nodes — the Merlin importer produces exactly
        // this shape — and the hierarchy view is where they are edited. Seeding a
        // group means that view is exercised in dev instead of always showing a
        // flat list, which is how its missing add-group affordance went unnoticed.
        // Note the mixed root list (a group AND leaves) needs `addRootLeaves`,
        // which takes List<DraftEstimationNode>; `addRoots` is group-only.
        addRootLeaves(
            draft, listOf(
                group(
                    draft, "Ingest", listOf(
                        bucketedLeaf(
                            draft, "Ingest connector A", bucketS,
                            isSample = true, min = 1.0, exp = 2.0, max = 3.0
                        ),
                        bucketedLeaf(draft, "Ingest connector B", bucketS, isSample = false)
                    )
                ),
                group(
                    draft, "Transform", listOf(
                        bucketedLeaf(
                            draft, "Transform pipeline X", bucketM,
                            isSample = true, min = 3.0, exp = 5.0, max = 8.0
                        ),
                        bucketedLeaf(draft, "Transform pipeline Y", bucketM, isSample = false)
                    )
                ),
                bucketedLeaf(draft, "Reporting dashboard", bucketL, isSample = true, min = 5.0, exp = 8.0, max = 13.0),
                bucketedLeaf(draft, "Data catalog", bucketL, isSample = false)
            )
        )

        draftRepository.persist(draft)

        Log.info(
            "Seeded bucket+sampled estimation ${estimation.id} " +
                "(method=${estimation.method}, buckets=${estimation.buckets.size})"
        )

        // No schedule graph here, by design (see the class KDoc).
        return 0
    }
}
