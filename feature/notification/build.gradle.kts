plugins {
    alias(libs.plugins.pacedream.android.library.compose)
    alias(libs.plugins.pacedream.android.feature)
}

android {
    namespace = "com.shourov.apps.pacedream.feature.notification"
}

dependencies {
    implementation(libs.accompanist.permissions)
    implementation(libs.timber)
    implementation(libs.retrofit.core)
    implementation(projects.common)
    implementation(projects.core.data)
    implementation(projects.core.network)
    implementation(projects.core.common)
}