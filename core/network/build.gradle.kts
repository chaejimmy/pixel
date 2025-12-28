plugins {
    alias(libs.plugins.pacedream.android.library)
    alias(libs.plugins.pacedream.android.hilt)
    alias(libs.plugins.pacedream.android.library.jacoco)
    id("kotlinx-serialization")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.shourov.apps.pacedream.core.network"

    buildFeatures {
        buildConfig = true
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