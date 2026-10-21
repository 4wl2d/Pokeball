dependencies {
    implementation(project(":benchmark:env"))
    // Only the Elm-style runtime (package bench.mvu.kit) is used from the naive module.
    implementation(project(":benchmark:mvu"))
}
