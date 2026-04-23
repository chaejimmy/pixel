plugins {
    alias(libs.plugins.pacedream.android.library.compose)
    alias(libs.plugins.pacedream.android.feature)
}

android {
    namespace = "com.shourov.apps.pacedream.feature.wifi"
}

dependencies {
    implementation(libs.timber)
    implementation(libs.retrofit.core)
    // Gson annotations — the Wi-Fi session DTOs use @SerializedName to
    // keep Retrofit's Gson converter wiring working across the feature.
    implementation(libs.retrofit.gson)
    implementation(projects.common)
    implementation(projects.core.network)
    implementation(projects.core.common)
}
