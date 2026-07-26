import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    kotlin("plugin.noarg")
    id("io.quarkus")
    id("io.gitlab.arturbosch.detekt")
}

repositories {
    mavenCentral()
    mavenLocal()
}

val quarkusPlatformGroupId = project.property("quarkusPlatformGroupId") as String
val quarkusPlatformArtifactId = project.property("quarkusPlatformArtifactId") as String
val quarkusPlatformVersion = project.property("quarkusPlatformVersion") as String

dependencies {
    implementation(enforcedPlatform("$quarkusPlatformGroupId:$quarkusPlatformArtifactId:$quarkusPlatformVersion"))

    // Shared KMP domain (JVM target), the single source of truth.
    implementation(project(":domain"))
    // The KMP domain logs via kotlin-logging, which on the JVM delegates to
    // slf4j (captured by Quarkus' JBoss LogManager bridge). The project
    // dependency does not propagate the domain's Gradle deps, so declare it.
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")

    implementation("io.quarkus:quarkus-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-flyway")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-container-image-jib")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-websockets-next")
    implementation("io.quarkus:quarkus-jackson")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.quarkus:quarkus-oidc")
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-logging-json")
    implementation("io.quarkus:quarkus-arc")
    implementation("org.apache.poi:poi-ooxml:5.4.1")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
}

// Open classes for the annotations Quarkus/Hibernate/JAX-RS require (mirrors
// the former kotlin-maven-plugin all-open / no-arg configuration verbatim).
allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.enterprise.context.RequestScoped")
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

noArg {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        javaParameters.set(true)
    }
}

tasks.test {
    useJUnitPlatform()
    // Quarkus requires the JBoss LogManager during tests.
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}

// Static analysis: detekt over the backend Kotlin sources. Reports are
// informational; the build stays green regardless of findings.
detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    ignoreFailures = true
    source.setFrom(files("src/main/kotlin"))
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/detekt/detekt.xml"))
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.file("reports/detekt/detekt.html"))
        sarif.required.set(true)
        sarif.outputLocation.set(layout.buildDirectory.file("reports/detekt/detekt.sarif"))
    }
}
