dependencies {
    // An adapter module: it implements the payments feature's provider ports over HTTP.
    implementation(project(":examples:shop:payments"))
    implementation(project(":pokeball-runtime"))
    testImplementation(project(":pokeball-testkit"))
}

tasks.test {
    systemProperty("pokeball.results", layout.buildDirectory.dir("reports/conformance").get().asFile.absolutePath)
}
