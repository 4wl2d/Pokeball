dependencies {
    // The simulated clock comes from the Ballarch test kit; the implementations do not use it directly.
    api(project(":ballarch-testkit"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}
