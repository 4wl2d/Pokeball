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

// Architecture benchmark: the same checkout in six styles, plus the runner (sources in /benchmark).
for (m in listOf("env", "layered", "hardened", "actors", "mvu", "saga", "ballarch", "runner")) {
    include(":benchmark:$m")
    project(":benchmark:$m").projectDir = file("../../benchmark/$m")
}
