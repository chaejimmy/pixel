plugins {
    alias(libs.plugins.pacedream.android.feature)
    alias(libs.plugins.pacedream.android.library.compose)
    alias(libs.plugins.pacedream.android.hilt)
    id("kotlinx-serialization")
}

android {
    namespace = "com.shourov.apps.pacedream.feature.webflow"
}

dependencies {
    implementation(projects.core.network)
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(projects.core.designsystem)
    implementation(projects.common)
    
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.browser)
    implementation(libs.coil.kt.compose)
    implementation(libs.timber)
}


