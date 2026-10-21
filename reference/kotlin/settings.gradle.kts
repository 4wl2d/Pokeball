rootProject.name = "ballarch-reference"

dependencyResolutionManagement {
    repositories { mavenCentral() }
}

// Library
include(":ballarch-kernel", ":ballarch-runtime", ":ballarch-testkit", ":ballarch-archrules")

// Examples and executable documentation
include(
    ":examples:order-draft",
    ":examples:tutorial",
    ":examples:trip",
    ":examples:itinerary",
    ":examples:catalog",
    ":examples:shop:payments-api",
    ":examples:shop:payments",
    ":examples:shop:payments-http",
    ":examples:shop:inventory-api",
    ":examples:shop:inventory",
    ":examples:shop:orders-api",
    ":examples:shop:orders",
    ":examples:shop:checkout",
    ":examples:shop:app",
)

// Architecture benchmark: the same checkout in four styles, each written naively and carefully, and in
// Ballarch, plus the simulation harness and the runner (sources in /benchmark).
for (m in listOf("env", "harness", "layered", "hardened", "actors", "actors-careful", "mvu", "mvu-careful", "saga", "saga-careful", "ballarch", "runner")) {
    include(":benchmark:$m")
    project(":benchmark:$m").projectDir = file("../../benchmark/$m")
}
// The change requests' runner (benchmark/changes).
include(":benchmark:changes")
project(":benchmark:changes").projectDir = file("../../benchmark/changes/runner")
