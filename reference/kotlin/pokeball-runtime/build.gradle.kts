plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
}

kotlin {
    jvmToolchain(21)
    explicitApi()
}

dependencies {
    api(project(":pokeball-kernel"))
    testImplementation(project(":pokeball-testkit"))
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test { useJUnitPlatform() }

// Optional mutation testing of the runtime: ./gradlew :pokeball-runtime:pitest
configure<info.solidsoft.gradle.pitest.PitestPluginExtension> {
    pitestVersion.set("1.30.0")
    junit5PluginVersion.set("1.2.3")
    targetClasses.set(listOf("pokeball.runtime.*"))
    threads.set(4)
    outputFormats.set(listOf("HTML", "XML"))
    timestampedReports.set(false)
    mutators.set(listOf("DEFAULTS"))
    timeoutConstInMillis.set(10_000)
}
