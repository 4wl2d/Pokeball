plugins {
    application
}

val implementations = listOf("layered", "hardened", "actors", "mvu", "saga", "ballarch")

dependencies {
    implementation(project(":benchmark:env"))
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
