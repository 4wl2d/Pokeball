dependencies {
    implementation(project(":benchmark:env"))
    // Only the transactional-messaging toolkit (package bench.saga.kit) is used from the naive module.
    implementation(project(":benchmark:saga"))
}
