plugins {
    alias(libs.plugins.pacedream.android.library)
    alias(libs.plugins.pacedream.android.room)
    alias(libs.plugins.pacedream.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.shourov.apps.pacedream.core.database"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)

    // Kotlinx
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.guava)

    // Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)

    // Migration test for PaceDreamDatabase (Phase A / P1-6).
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.junit.junit)
}
