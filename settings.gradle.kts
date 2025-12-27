pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "pacedream"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
include(":app")
include(":benchmarks")
include(":core:analytics")
include(":core:common")
include(":core:data")
include(":core:database")
include(":core:datastore")
include(":core:datastore-proto")
include(":core:designsystem")
include(":core:model")
include(":core:ui")
include(":core:network")

//include(":lint")
//include(":sync:work")
//include(":sync:sync-test")
//include(":ui-test-hilt-manifest")

include(":feature:signin")
include(":feature:booking")
include(":feature:payment")
include(":feature:guest")
include(":feature:notification")
include(":feature:create-account")
include(":feature:host")
include(":common")
include(":feature:home")
include(":feature:home:domain")
include(":feature:home:presentation")
include(":feature:home:data")
include(":feature:search:data")
include(":feature:search:domain")
include(":feature:search:presentation")
include(":feature:auth")
include(":feature:chat")
include(":feature:wishlist")
include(":feature:inbox")
include(":feature:webflow")
