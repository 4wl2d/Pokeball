plugins {
    application
}

// The change requests' runner: runs the scenario-A designs under each change request (benchmark/changes/README.md).
val implementations = listOf("layered", "hardened", "actors", "actors-careful", "mvu", "mvu-careful", "saga", "saga-careful", "ballarch")

dependencies {
    implementation(project(":benchmark:harness"))
    for (m in implementations) implementation(project(":benchmark:$m"))
}

application {
    mainClass.set("bench.changes.MainKt")
}

// ./gradlew :benchmark:changes:run --args="out.json 500 100 cr1-refund"
tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}
