plugins {
    alias(libs.plugins.pacedream.android.library.compose)
    alias(libs.plugins.pacedream.android.feature)
}

android {
    namespace = "com.shourov.apps.pacedream.feature.home"
    lint {
        checkReleaseBuilds = false
        abortOnError = false
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