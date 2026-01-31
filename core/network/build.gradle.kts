plugins {
    alias(libs.plugins.pacedream.android.library)
    alias(libs.plugins.pacedream.android.hilt)
    alias(libs.plugins.pacedream.android.library.jacoco)
    id("kotlinx-serialization")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

import java.util.Properties

android {
    namespace = "com.shourov.apps.pacedream.core.network"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        // Load secrets from properties file
        val secretsProperties = Properties()
        val secretsFile = rootProject.file("secrets.defaults.properties")
        if (secretsFile.exists()) {
            secretsFile.inputStream().use { secretsProperties.load(it) }
        }
        
        // Auth0 Configuration from secrets file
        val auth0Domain = secretsProperties.getProperty("AUTH0_DOMAIN") ?: "dev-pacedream.us.auth0.com"
        val auth0ClientId = secretsProperties.getProperty("AUTH0_CLIENT_ID") ?: ""
        val auth0Audience = secretsProperties.getProperty("AUTH0_AUDIENCE") ?: "https://$auth0Domain/api/v2/"
        
        buildConfigField("String", "AUTH0_DOMAIN", "\"$auth0Domain\"")
        buildConfigField("String", "AUTH0_CLIENT_ID", "\"$auth0ClientId\"")
        buildConfigField("String", "AUTH0_AUDIENCE", "\"$auth0Audience\"")
    }

}

secrets {
    defaultPropertiesFileName = "secrets.defaults.properties"
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