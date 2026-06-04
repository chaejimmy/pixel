plugins {
    alias(libs.plugins.pacedream.android.library)
}

android {
    namespace = "com.shourov.apps.pacedream.feature.notifications"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    testImplementation(libs.junit.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
}
