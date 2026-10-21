plugins {
    application
}

val implementations = listOf("layered", "hardened", "actors", "actors-careful", "mvu", "mvu-careful", "saga", "saga-careful", "ballarch")

dependencies {
    implementation(project(":benchmark:env"))
    implementation(project(":benchmark:harness"))
    implementation(project(":ballarch-runtime"))
    for (m in implementations) implementation(project(":benchmark:$m"))
    implementation("org.ow2.asm:asm-tree:9.8")
}

application {
    mainClass.set("bench.runner.MainKt")
}

// ./gradlew :benchmark:runner:run --args="1000 ../../benchmark/results"
tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
    for (m in implementations) {
        val p = project(":benchmark:$m")
        dependsOn("${p.path}:classes")
        systemProperty("bench.classes.$m", p.layout.buildDirectory.dir("classes/kotlin/main").get().asFile.absolutePath)
    }
}

// Machine-dependent, so kept out of results.json; writes ../../benchmark/results/overhead.json: ./gradlew :benchmark:runner:overhead
tasks.register<JavaExec>("overhead") {
    group = "benchmark"
    description = "Measures a request through the reference runtime against a direct call of the same decider."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("bench.runner.OverheadKt")
    workingDir = rootProject.projectDir
}
