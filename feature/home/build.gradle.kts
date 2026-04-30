plugins {
    alias(libs.plugins.pacedream.android.library.compose)
    alias(libs.plugins.pacedream.android.feature)
}

android {
    namespace = "com.shourov.apps.pacedream.feature.home"

    // This module is an empty container that aggregates the sub-modules
    // (:feature:home:presentation, :data, :domain). It has no src/ of its own,
    // and lint's checkBuildScripts pulls KSP-generated Java sources from those
    // sub-modules as Java roots, crashing with FileNotFoundException when KSP
    // hasn't run yet. Real lint coverage still happens in the sub-modules.
    lint {
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(libs.accompanist.permissions)
    implementation(projects.core.data)
    implementation(libs.country.code.picker)
    implementation(libs.coil.kt.compose)
//    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.auth)
    implementation(project(":common"))
    implementation(project(":core:network"))
    implementation(project(":core:ui"))

}