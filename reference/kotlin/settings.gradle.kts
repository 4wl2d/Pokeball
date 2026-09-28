rootProject.name = "pokeball-reference"

dependencyResolutionManagement {
    repositories { mavenCentral() }
}

// Library
include(":pokeball-kernel", ":pokeball-runtime", ":pokeball-testkit", ":pokeball-archrules")

// Examples and executable documentation
include(
    ":examples:order-draft",
    ":examples:tutorial",
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
