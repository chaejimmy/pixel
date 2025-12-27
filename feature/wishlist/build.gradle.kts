plugins {
    alias(libs.plugins.pacedream.android.feature)
    alias(libs.plugins.pacedream.android.library.compose)
    alias(libs.plugins.pacedream.android.hilt)
    id("kotlinx-serialization")
}

android {
    namespace = "com.shourov.apps.pacedream.feature.wishlist"
}

dependencies {
    implementation(projects.core.network)
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(projects.core.designsystem)
    implementation(projects.common)
    
    implementation(libs.coil.kt.compose)
    implementation(libs.kotlinx.serialization.json)
}

