plugins {
    alias(libs.plugins.pacedream.android.feature)
    alias(libs.plugins.pacedream.android.library.compose)
    alias(libs.plugins.pacedream.android.hilt)
    id("kotlinx-serialization")
}

android {
    namespace = "com.shourov.apps.pacedream.feature.wanted"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(projects.core.network)
    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(projects.core.designsystem)
    implementation(projects.core.upload)
    implementation(projects.core.location)
    implementation(projects.common)

    implementation(libs.androidx.activity.compose)
    implementation(libs.coil.kt.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.timber)

    testImplementation(libs.junit.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test)
    debugImplementation(libs.androidx.compose.ui.testManifest)
}
