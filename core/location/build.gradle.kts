plugins {
    alias(libs.plugins.pacedream.android.library)
    alias(libs.plugins.pacedream.android.library.compose)
    alias(libs.plugins.pacedream.android.hilt)
}

android {
    namespace = "com.shourov.apps.pacedream.core.location"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.network)

    implementation(libs.kotlinx.coroutines.play.services)
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // CurrentLocationController is a Compose helper — needs the activity-compose
    // permission launcher and runtime/ui packages from the Compose BOM
    // (BOM platform is added by the android.library.compose convention plugin).
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.core.ktx)

    implementation(libs.timber)

    testImplementation(libs.junit.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
