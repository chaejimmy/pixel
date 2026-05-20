plugins {
    alias(libs.plugins.pacedream.android.library.compose)
    alias(libs.plugins.pacedream.android.feature)
}

android {
    namespace = "com.shourov.apps.pacedream.feature.signin"
}

dependencies {
    implementation(libs.accompanist.permissions)
    implementation(projects.core.data)
    implementation(libs.country.code.picker)
//    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.auth)
    implementation(libs.retrofit.gson)
    implementation(libs.timber)
    implementation(project(":common"))
    implementation(project(":core:network"))
    implementation(project(":core:ui"))

    testImplementation(libs.junit.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test)
    testImplementation(libs.androidx.navigation.testing)
    debugImplementation(libs.androidx.compose.ui.testManifest)
}