plugins {
    alias(libs.plugins.pacedream.android.library)
    alias(libs.plugins.pacedream.android.library.jacoco)
    alias(libs.plugins.pacedream.android.hilt)
}

android {
    namespace = "com.shourov.apps.pacedream.core.common"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        // Roommate Finder is removed from the tree (audit Finding 4). The
        // flag stays so a future re-introduction can flip it from a real
        // implementation PR. Release default is `false`; the debug build
        // type overrides to `true` so the route is available for
        // in-progress dev builds.
        buildConfigField("boolean", "FEATURE_ROOMMATE_FINDER", "false")
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "FEATURE_ROOMMATE_FINDER", "true")
        }
    }
}

dependencies {
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    implementation(libs.androidx.lifecycle.viewModelCompose)
}