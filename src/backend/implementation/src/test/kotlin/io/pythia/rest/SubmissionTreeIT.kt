package io.pythia.rest

import io.pythia.domain.Estimation
import io.pythia.domain.Project
import io.pythia.repository.EstimationRepository
import io.pythia.auth.DevAdminAuth
import io.pythia.repository.ProjectRepository
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.extension.ExtendWith
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

@QuarkusTest
@ExtendWith(DevAdminAuth::class)
class SubmissionTreeIT {

    @Inject
    lateinit var projectRepository: ProjectRepository

    @Inject
    lateinit var estimationRepository: EstimationRepository

    @Inject
    lateinit var entityManager: EntityManager

    private lateinit var estimationId: UUID

    @BeforeEach
    @Transactional
    fun setup() {
        val project = Project().apply { name = "Test Project" }
        projectRepository.persist(project)
        val estimation = Estimation().apply {
            offer = "TREE-001"
            this.project = project
        }
        estimationRepository.persist(estimation)
        estimationId = estimation.id!!
    }

    @Test
    fun `submitting Group of Group of Items plus a sibling Item persists the tree shape`() {
        given().post("/api/estimations/$estimationId/versions").then().statusCode(201)

        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                    "stdDevFactor": 0.0,
                    "roots": [{
                        "type": "GROUP",
                        "title": "Backend",
                        "children": [
                            {
                                "type": "GROUP",
                                "title": "Auth",
                                "children": [
                                    {"type": "FIXED", "description": "Token endpoint", "minEffort": 1.0, "expectedEffort": 2.0, "maxEffort": 3.0},
                                    {"type": "FIXED", "description": "Session storage", "minEffort": 2.0, "expectedEffort": 4.0, "maxEffort": 6.0}
                                ]
                            },
                            {"type": "FIXED", "description": "Health endpoint", "minEffort": 1.0, "expectedEffort": 1.0, "maxEffort": 1.0}
                        ]
                    }]
                }
            """.trimIndent()
            )
            .put("/api/estimations/$estimationId/versions/draft").then().statusCode(200)

        given().post("/api/estimations/$estimationId/versions/draft/submit").then().statusCode(200)

        val versionId = given().get("/api/estimations/$estimationId/versions/1")
            .then().statusCode(200).extract().jsonPath().getString("versionNumber")
            .let { resolveVersionId(estimationId, it.toInt()) }

        val rows = nodeRowsForVersion(versionId)

        // 1 root group + 1 nested group + 2 leaves under Auth + 1 sibling leaf = 5 rows
        assertEquals(5, rows.size, "expected 5 nodes total, got ${rows.size}")

        val byTypeAndPos = rows.groupBy { it.nodeType }
        val groups = byTypeAndPos["GROUP"] ?: emptyList()
        val fixedLeaves = byTypeAndPos["FIXED"] ?: emptyList()
        assertEquals(2, groups.size, "expected 2 GROUP rows")
        assertEquals(3, fixedLeaves.size, "expected 3 FIXED rows")

        // Exactly one root (parent_id IS NULL); it must be a GROUP.
        val roots = rows.filter { it.parentId == null }
        assertEquals(1, roots.size, "expected exactly one root row")
        val root = roots.single()
        assertEquals("GROUP", root.nodeType)
        assertEquals(0, root.position)

        // Root has two direct children at positions 0 and 1.
        val rootChildren = rows.filter { it.parentId == root.id }.sortedBy { it.position }
        assertEquals(2, rootChildren.size)
        assertEquals(listOf(0, 1), rootChildren.map { it.position })

        // The GROUP child (Auth) has two leaf children at positions 0 and 1.
        val authGroup = rootChildren.first { it.nodeType == "GROUP" }
        val authChildren = rows.filter { it.parentId == authGroup.id }.sortedBy { it.position }
        assertEquals(2, authChildren.size)
        assertTrue(authChildren.all { it.nodeType == "FIXED" })

        // GROUP rows' offer_pt equals the SUM of their subtree's leaf offer_pt.
        // With stdDevFactor=0 and no drivers, offerPT == mean (PERT).
        val authLeafOfferPtSum = authChildren.sumOf { it.offerPt }
        assertEquals(authLeafOfferPtSum, authGroup.offerPt, 0.001)
        // Auth = 2.0 + 4.0 = 6.0; Health = 1.0; Backend = 6.0 + 1.0 = 7.0
        assertEquals(6.0, authGroup.offerPt, 0.001)
        assertEquals(7.0, root.offerPt, 0.001)

        // The sibling leaf carries the expected fields.
        val healthLeaf = rootChildren.first { it.nodeType == "FIXED" }
        assertEquals(1.0, healthLeaf.offerPt, 0.001)
        assertNull(healthLeaf.title)
        assertNotNull(healthLeaf.description)
    }

    data class NodeRow(
        val id: UUID,
        val parentId: UUID?,
        val nodeType: String,
        val position: Int,
        val title: String?,
        val description: String?,
        val offerPt: Double
    )

    @Transactional
    fun nodeRowsForVersion(versionId: UUID): List<NodeRow> {
        // UUID columns are cast to text on read for easy mapping; the WHERE
        // parameter is bound as a UUID (PostgreSQL rejects uuid = varchar).
        // Claude thinks that using native queries here is a good thing, because
        // a) it is only in test code
        // b) it checks the JPA layer too
        @Suppress("UNCHECKED_CAST")
        val raw = entityManager.createNativeQuery(
            "SELECT CAST(id AS VARCHAR), CAST(parent_id AS VARCHAR), node_type, position, title, description, offer_pt " +
                    "FROM submitted_estimation_nodes WHERE version_id = :vid"
        )
            .setParameter("vid", versionId)
            .resultList as List<Array<Any?>>
        return raw.map { row ->
            NodeRow(
                id = UUID.fromString(row[0] as String),
                parentId = (row[1] as String?)?.let { UUID.fromString(it) },
                nodeType = row[2] as String,
                position = (row[3] as Number).toInt(),
                title = row[4] as String?,
                description = row[5] as String?,
                offerPt = (row[6] as Number).toDouble()
            )
        }
    }

    @Transactional
    fun resolveVersionId(estimationId: UUID, versionNumber: Int): UUID {
        val s = entityManager.createNativeQuery(
            "SELECT CAST(id AS VARCHAR) FROM submitted_estimation_versions WHERE estimation_id = :eid AND version_number = :vn"
        )
            .setParameter("eid", estimationId)
            .setParameter("vn", versionNumber)
            .singleResult as String
        return UUID.fromString(s)
    }
}
