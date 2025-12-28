plugins {
    alias(libs.plugins.pacedream.android.library.compose)
    alias(libs.plugins.pacedream.android.feature)
}

android {
    namespace = "com.shourov.apps.pacedream.feature.chat"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":common"))

    implementation(libs.androidx.activity.compose)
}
