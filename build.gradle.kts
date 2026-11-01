import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import java.util.Locale

plugins {
    java
    jacoco
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.errorprone)
    alias(libs.plugins.sonarqube)
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
    implementation(libs.spring.boot.starter.security.oauth2.resource.server)
    implementation(libs.bouncycastle.bcprov)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.bucket4j.core)
    implementation(libs.caffeine)
    implementation(libs.jspecify)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.spring.boot.starter.mail)
    implementation(libs.spring.boot.starter.thymeleaf)
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
    systemProperty("user.timezone", "America/Sao_Paulo")
}

tasks.compileJava {
    options.errorprone {
        disableAllChecks = true
        check("NullAway", CheckSeverity.ERROR)
        option("NullAway:AnnotatedPackages", "br.com.arktech.workshop")
        option("NullAway:JSpecifyMode", "true")
        option("NullAway:ExternalInitAnnotations", "jakarta.persistence.Entity,jakarta.persistence.MappedSuperclass")
        option("NullAway:ExcludedFieldAnnotations", "jakarta.persistence.Id")
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

val coverageScope = listOf(
    "br/com/arktech/workshop/*/domain/**",
    "br/com/arktech/workshop/*/application/**",
)

val coverageSummary = tasks.register("coverageSummary") {
    description = "Writes the line coverage of the domain and application layers as a Markdown table."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    val csv = layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.csv")
    val summary = layout.buildDirectory.file("reports/jacoco/test/coverage-summary.md")
    val minimum = providers.gradleProperty("coverageLineMinimum").map { it.toBigDecimal() }
    inputs.files(csv)
    inputs.property("minimum", minimum)
    outputs.file(summary)
    onlyIf { csv.get().asFile.exists() }

    doLast {
        val locale = Locale.of("pt", "BR")
        val modules = sortedMapOf<String, LongArray>()
        val scope = LongArray(2)
        val project = LongArray(2)
        csv.get().asFile.readLines().drop(1).filter { it.isNotBlank() }.forEach { line ->
            val columns = line.split(",")
            val missed = columns[7].toLong()
            val covered = columns[8].toLong()
            val packageName = columns[1].split(".")
            project[0] += missed
            project[1] += covered
            if (packageName.getOrNull(5) in setOf("domain", "application")) {
                val module = modules.getOrPut(packageName[4]) { LongArray(2) }
                module[0] += missed
                module[1] += covered
                scope[0] += missed
                scope[1] += covered
            }
        }

        fun lines(counts: LongArray) = String.format(locale, "%,d", counts[0] + counts[1])
        fun ratio(counts: LongArray) =
            if (counts[0] + counts[1] == 0L) 1.0 else counts[1].toDouble() / (counts[0] + counts[1])
        fun percent(counts: LongArray) = String.format(locale, "%.1f%%", 100 * ratio(counts))

        val passed = ratio(scope).toBigDecimal() >= minimum.get()
        val markdown = buildString {
            appendLine("Regra: linhas de `domain` e `application` de todos os módulos, mínimo ${minimum.get().movePointRight(2).stripTrailingZeros().toPlainString()}%.")
            appendLine()
            appendLine("| Módulo | Linhas | Cobertura |")
            appendLine("|---|---:|---:|")
            modules.forEach { (name, counts) -> appendLine("| $name | ${lines(counts)} | ${percent(counts)} |") }
            appendLine("| **Escopo da regra** | **${lines(scope)}** | **${percent(scope)}** ${if (passed) "✅" else "❌"} |")
            appendLine()
            appendLine("Projeto inteiro: ${percent(project)} das linhas (${lines(project)}).")
        }
        summary.get().asFile.writeText(markdown)
    }
}

tasks.jacocoTestReport {
    reports {
        csv.required = true
    }
    finalizedBy(coverageSummary)
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    classDirectories.setFrom(sourceSets.main.get().output.classesDirs.asFileTree.matching { include(coverageScope) })
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = providers.gradleProperty("coverageLineMinimum").get().toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.register<Test>("integrationTest") {
    description = "Runs only the integration tests."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { includeTags("integration") }
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
    }
}

sonar {
    properties {
        property("sonar.projectKey", "workshop-service")
        property("sonar.host.url", providers.environmentVariable("SONAR_HOST_URL").getOrElse("http://localhost:9000"))
    }
}
