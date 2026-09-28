dependencies {
    implementation(project(":pokeball-kernel"))
    // Only the composition root (package ...catalog.app) uses the runtime.
    implementation(project(":pokeball-runtime"))
    testImplementation(project(":pokeball-testkit"))
}
