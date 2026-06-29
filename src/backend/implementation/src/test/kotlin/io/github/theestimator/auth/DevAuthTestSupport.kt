package io.github.theestimator.auth

import io.restassured.RestAssured
import io.restassured.builder.RequestSpecBuilder
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/** The dev module is strict (no default-user fallback), so RestAssured-based
 *  ITs must authenticate. This header logs in as the all-roles dev-admin. */
const val DEV_ADMIN_AUTH_HEADER = "Dev dev-admin"

/**
 * JUnit5 extension that installs a global RestAssured request specification
 * adding the dev-admin `Authorization` header, so every `given()` call in an
 * annotated IT (including those in `@BeforeEach setup()`) is authenticated.
 * Reset after each test so the global spec does not leak across classes.
 */
class DevAdminAuth : BeforeEachCallback, AfterEachCallback {
    override fun beforeEach(context: ExtensionContext) {
        RestAssured.requestSpecification = RequestSpecBuilder()
            .addHeader("Authorization", DEV_ADMIN_AUTH_HEADER)
            .build()
    }

    override fun afterEach(context: ExtensionContext) {
        RestAssured.requestSpecification = null
    }
}
