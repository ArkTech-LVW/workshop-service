import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    jacoco
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.errorprone)
}

group = "br.com.arktech"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

springBoot {
    // Generates META-INF/build-info.properties, used as the API version in the OpenAPI docs
    buildInfo()
}

dependencies {
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.jspecify)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.flyway.database.postgresql)
    runtimeOnly(libs.postgresql)
    developmentOnly(libs.spring.boot.docker.compose)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    errorprone(libs.errorprone.core)
    errorprone(libs.nullaway)

    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.actuator.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.archunit.junit5)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Run tests with a non pt-BR JVM locale (like Docker/CI) so i18n defaults are actually exercised
    systemProperty("user.language", "en")
    systemProperty("user.country", "US")
}

tasks.compileJava {
    options.errorprone {
        disableAllChecks = true
        check("NullAway", CheckSeverity.ERROR)
        option("NullAway:AnnotatedPackages", "br.com.arktech.workshop")
        option("NullAway:JSpecifyMode", "true")
    }
}

tasks.compileTestJava {
    options.errorprone.enabled = false
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
    addTestListener(object : TestListener {
        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent != null) return

            val skipped = if (result.skippedTestCount > 0) ", ${result.skippedTestCount} skipped" else ""
            logger.lifecycle(
                "Tests: ${result.testCount} run, ${result.successfulTestCount} passed, ${result.failedTestCount} failed$skipped"
            )
        }
    })
}
