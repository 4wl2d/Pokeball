dependencies {
    implementation(project(":benchmark:env"))
    // Only the actor toolkit (package bench.actors.kit) is used from the naive module.
    implementation(project(":benchmark:actors"))
}
