plugins {
    alias(libs.plugins.pacedream.android.library)
    alias(libs.plugins.pacedream.android.library.jacoco)
    alias(libs.plugins.pacedream.android.hilt)
    id("kotlinx-serialization")
}

android {
    namespace = "com.shourov.apps.pacedream.core.data"
    buildFeatures {
        buildConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    api(projects.core.common)
    api(projects.core.database)
    api(projects.core.datastore)
    api(projects.core.network)
    implementation(libs.gson.convert)
    implementation(libs.retrofit.gson)
    implementation(libs.kotlinx.serialization.json)

    implementation(projects.core.analytics)
    implementation(libs.play.services.auth.api.phone)
}
