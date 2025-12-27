plugins {
    alias(libs.plugins.pacedream.android.library)
    alias(libs.plugins.pacedream.android.hilt)
    alias(libs.plugins.pacedream.android.room)
    id("kotlinx-serialization")
}

android {
    namespace = "com.shourov.apps.pacedream.core.database"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(projects.core.model)
    api(projects.core.common)
    
    // Kotlinx
    implementation(libs.kotlinx.datetime)
    
    // Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.moshi)
    implementation(libs.moshi.adapters)
    
    ksp(libs.moshi.codegen)
}
