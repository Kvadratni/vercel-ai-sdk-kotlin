plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    `maven-publish`
}

group = "dev.vercel"
version = "0.1.0"

repositories {
    mavenCentral()
}

// Define configurations for integration tests
val integrationTestSourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
    implementation("io.ktor:ktor-client-core:2.3.6")
    implementation("io.ktor:ktor-client-cio:2.3.6")
    implementation("io.ktor:ktor-client-okhttp:2.3.6")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.6")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.6")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("org.json:json:20231013")
    
    // Unit test dependencies
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("io.ktor:ktor-client-mock:2.3.6")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.slf4j:slf4j-simple:2.0.7")
    
    // Integration test dependencies - explicitly defined
    "integrationTestSourceSetImplementation"("org.jetbrains.kotlin:kotlin-test:2.0.0")
    "integrationTestSourceSetImplementation"("org.jetbrains.kotlin:kotlin-test-junit5:2.0.0")
    "integrationTestSourceSetImplementation"("org.junit.jupiter:junit-jupiter:5.9.3")
    "integrationTestSourceSetImplementation"("io.mockk:mockk:1.13.5")
    "integrationTestSourceSetImplementation"("io.ktor:ktor-client-mock:2.3.6")
    "integrationTestSourceSetImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    "integrationTestSourceSetImplementation"("org.slf4j:slf4j-simple:2.0.7")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    
    // Common test configuration
    maxParallelForks = Runtime.getRuntime().availableProcessors().div(2).takeIf { it > 0 } ?: 1
    
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showExceptions = true
        showStackTraces = true
        showCauses = true
        showStandardStreams = true
    }
}

// Unit test specific configuration
tasks.test {
    description = "Runs unit tests."
    
    // Configure timeouts for unit tests
    systemProperty("junit.jupiter.execution.timeout.default", "30s")
    systemProperty("junit.jupiter.execution.timeout.testable.method.default", "30s")
    
    // Enable ByteBuddy experimental support for Java 21
    jvmArgs("-Dnet.bytebuddy.experimental=true")
    
    filter {
        // Exclude integration tests from unit test task
        excludeTestsMatching("*.integration.*")
    }
}

// Integration test configuration
val runIntegrationTests by tasks.registering(Test::class) {
    description = "Runs integration tests."
    group = "verification"
    
    testClassesDirs = integrationTestSourceSet.output.classesDirs
    classpath = integrationTestSourceSet.runtimeClasspath
    
    // Integration test specific settings
    systemProperty("junit.jupiter.execution.timeout.default", "60s")
    systemProperty("junit.jupiter.execution.timeout.testable.method.default", "60s")
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    
    // Enable ByteBuddy experimental support for Java 21
    jvmArgs("-Dnet.bytebuddy.experimental=true")
    
    // Only run tests in integration test packages
    filter {
        includeTestsMatching("*.integration.*")
    }
    
    mustRunAfter(tasks.test)
    
    // Ensure integration tests are always run if explicitly requested
    outputs.upToDateWhen { false }
}

kotlin {
    jvmToolchain(21)
}
