plugins {
    alias(libs.plugins.pacedream.android.library)
    alias(libs.plugins.pacedream.android.hilt)
}

android {
    namespace = "com.shourov.apps.pacedream.core.upload"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.network)

    implementation(libs.timber)

    testImplementation(libs.junit.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
