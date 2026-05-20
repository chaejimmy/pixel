plugins {
    alias(libs.plugins.pacedream.android.library)
    alias(libs.plugins.pacedream.android.library.compose)
    alias(libs.plugins.pacedream.android.library.jacoco)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.shourov.apps.pacedream.designsystem"
}

dependencies {
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.foundation.layout)
    api(libs.androidx.compose.material.iconsExtended)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material3.adaptive)
    api(libs.androidx.compose.material3.navigationSuite)
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui.util)

    implementation(libs.coil.kt.compose)
    implementation(libs.androidx.ui.text.google.fonts)

    // Provides PaceDreamElevation / PaceDreamRadius tokens used as the
    // adaptiveShadow defaults. `common` has no project dependencies so this
    // does not create a cycle.
    implementation(project(":common"))
}
