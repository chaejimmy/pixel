plugins {
    alias(libs.plugins.pacedream.android.library)
    alias(libs.plugins.pacedream.android.hilt)
    alias(libs.plugins.pacedream.android.library.jacoco)
    id("kotlinx-serialization")
    // NOTE: secrets-gradle-plugin removed from this module.
    // BuildConfig fields are generated manually below with proper empty-string quoting.
    // The plugin cannot handle empty property values (generates `= ;` instead of `= ""`).
}

import java.util.Properties

android {
    namespace = "com.shourov.apps.pacedream.core.network"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        // Load secrets: defaults first, then local overrides (secrets.properties wins)
        val secretsProperties = Properties()
        listOf("secrets.defaults.properties", "secrets.properties").forEach { name ->
            val f = rootProject.file(name)
            if (f.exists()) f.inputStream().use { secretsProperties.load(it) }
        }
        
        // Auth0 Configuration from secrets file
        val auth0Domain = secretsProperties.getProperty("AUTH0_DOMAIN") ?: "" // Set in secrets.defaults.properties
        val auth0ClientId = secretsProperties.getProperty("AUTH0_CLIENT_ID") ?: "" // Set in secrets.defaults.properties
        val auth0Audience = secretsProperties.getProperty("AUTH0_AUDIENCE") ?: "https://$auth0Domain/api/v2/"
        
        buildConfigField("String", "AUTH0_DOMAIN", "\"$auth0Domain\"")
        buildConfigField("String", "AUTH0_CLIENT_ID", "\"$auth0ClientId\"")
        buildConfigField("String", "AUTH0_AUDIENCE", "\"$auth0Audience\"")

        // Frontend URL (iOS parity: FRONTEND_BASE_URL in xcconfig)
        val frontendUrl = secretsProperties.getProperty("FRONTEND_BASE_URL") ?: "https://www.pacedream.com"
        buildConfigField("String", "FRONTEND_BASE_URL", "\"$frontendUrl\"")

        // Cloudinary config (iOS parity: CLOUDINARY_CLOUD_NAME / CLOUDINARY_UPLOAD_PRESET in xcconfig)
        val cloudName = secretsProperties.getProperty("CLOUDINARY_CLOUD_NAME") ?: ""
        val uploadPreset = secretsProperties.getProperty("CLOUDINARY_UPLOAD_PRESET") ?: ""
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"$cloudName\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"$uploadPreset\"")
    }

}

dependencies {
    api(libs.kotlinx.datetime)
    api(projects.core.common)
    api(projects.core.model)

    implementation(libs.coil.kt)
    implementation(libs.coil.kt.svg)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)
    implementation(libs.androidx.security.crypto)
//    implementation(projects.core.database)
//    implementation(libs.androidx.paging.compose)
//    implementation(libs.androidx.paging.runtime.ktx)
//    implementation(libs.room.ktx)

    implementation(libs.timber)
//    implementation(libs.koin.android)
    implementation(libs.moshi)
    implementation(libs.moshi.adapters)
    implementation(libs.androidx.junit.ktx)
    androidTestImplementation(libs.junit.junit)

    ksp(libs.moshi.codegen)

    implementation(libs.gson.convert)
    
    // Auth0 SDK
    implementation(libs.auth0)
}