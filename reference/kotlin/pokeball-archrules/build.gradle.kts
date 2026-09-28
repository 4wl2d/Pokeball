plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
    explicitApi()
}

dependencies {
    api(project(":pokeball-kernel"))
    api("com.tngtech.archunit:archunit:1.4.1")
    implementation(kotlin("reflect"))
    testImplementation(project(":pokeball-runtime"))
    // The rules are also run against the real examples.
    testImplementation(project(":examples:order-draft"))
    testImplementation(project(":examples:catalog"))
    testImplementation(project(":examples:shop:checkout"))
    testImplementation(project(":examples:shop:payments"))
    testImplementation(project(":examples:shop:inventory"))
    testImplementation(project(":examples:shop:orders"))
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("pokeball.results", layout.buildDirectory.dir("reports/conformance").get().asFile.absolutePath)
}
