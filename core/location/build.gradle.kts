plugins {
    alias(libs.plugins.pacedream.android.library)
    alias(libs.plugins.pacedream.android.hilt)
}

android {
    namespace = "com.shourov.apps.pacedream.core.location"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.network)

    implementation(libs.kotlinx.coroutines.play.services)
    implementation("com.google.android.gms:play-services-location:21.0.1")

    implementation(libs.timber)

    testImplementation(libs.junit.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
