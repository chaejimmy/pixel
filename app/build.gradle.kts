import com.shourov.apps.pacedream.PaceDreamBuildType

plugins {
    alias(libs.plugins.pacedream.android.application)
    alias(libs.plugins.pacedream.android.application.compose)
    alias(libs.plugins.pacedream.android.application.flavors)
    alias(libs.plugins.pacedream.android.application.jacoco)
    alias(libs.plugins.pacedream.android.hilt)
    alias(libs.plugins.pacedream.android.application.firebase)
    id("com.google.android.gms.oss-licenses-plugin")
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.kotlin.serialization)
}

android {
    defaultConfig {
        applicationId = "com.shourov.apps.pacedream"
        versionCode = 8
        versionName = "0.1.2" // X.Y.Z; X = Major, Y = minor, Z = Patch level

        vectorDrawables {
            useSupportLibrary = true
        }
        
        // Auth0 manifest placeholders
        manifestPlaceholders["auth0Domain"] = "" // Set via local.properties or CI
        manifestPlaceholders["auth0Scheme"] = "pacedream"
        // Stripe publishable key (set in local.properties or CI as stripePublishableKey)
        buildConfigField("String", "STRIPE_PUBLISHABLE_KEY", "\"${(project.findProperty("stripePublishableKey") as? String) ?: ""}\"")
        // Auth0 client ID
        buildConfigField("String", "AUTH0_CLIENT_ID", "\"${(project.findProperty("auth0ClientId") as? String) ?: ""}\"")
        // OneSignal App ID (iOS parity: OneSignal_AppID in Info.plist)
        buildConfigField("String", "ONESIGNAL_APP_ID", "\"${(project.findProperty("onesignalAppId") as? String) ?: ""}\"")
    }

    // Release signing configuration.
    // For CI/CD or production builds, set these Gradle properties (via gradle.properties,
    // environment variables, or -P flags):
    //   RELEASE_STORE_FILE      - absolute path to the keystore file
    //   RELEASE_STORE_PASSWORD  - keystore password
    //   RELEASE_KEY_ALIAS       - key alias within the keystore
    //   RELEASE_KEY_PASSWORD    - key password
    //
    // Example (gradle.properties):
    //   RELEASE_STORE_FILE=/path/to/release.keystore
    //   RELEASE_STORE_PASSWORD=changeit
    //   RELEASE_KEY_ALIAS=my-key
    //   RELEASE_KEY_PASSWORD=changeit
    //
    // If these properties are not set the build falls back to the debug signing
    // config so that local development builds still work out of the box.
    val hasReleaseSigningProps = listOf(
        "RELEASE_STORE_FILE",
        "RELEASE_STORE_PASSWORD",
        "RELEASE_KEY_ALIAS",
        "RELEASE_KEY_PASSWORD",
    ).all { project.hasProperty(it) }

    if (hasReleaseSigningProps) {
        signingConfigs {
            create("release") {
                storeFile = file(project.property("RELEASE_STORE_FILE") as String)
                storePassword = project.property("RELEASE_STORE_PASSWORD") as String
                keyAlias = project.property("RELEASE_KEY_ALIAS") as String
                keyPassword = project.property("RELEASE_KEY_PASSWORD") as String
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = PaceDreamBuildType.DEBUG.applicationIdSuffix
        }
        release {
            isMinifyEnabled = true
            applicationIdSuffix = PaceDreamBuildType.RELEASE.applicationIdSuffix
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            signingConfig = if (hasReleaseSigningProps) {
                signingConfigs.getByName("release")
            } else {
                // Fall back to debug signing so local/dev builds can still be installed.
                signingConfigs.getByName("debug")
            }
            // Ensure Baseline Profile is fresh for release builds.
            baselineProfile.automaticGenerationDuringBuild = true
        }
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    buildFeatures {
        buildConfig = true
    }
    namespace = "com.shourov.apps.pacedream"
}

dependencies {

    // feature dependencies
    implementation(projects.feature.signin)
    implementation(projects.feature.booking)
    implementation(projects.feature.notification)
    implementation(projects.feature.guest)
    implementation(projects.feature.payment)
    implementation(projects.feature.createAccount)
    implementation(projects.feature.host)
    implementation(projects.feature.wishlist)
    implementation(projects.feature.inbox)
    implementation(projects.feature.webflow)
    implementation(project(":feature:auth"))

    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(projects.core.designsystem)
    implementation(projects.core.data)
    implementation(projects.core.network)
    implementation(projects.core.model)
    implementation(projects.core.analytics)
    implementation(projects.common)
//    implementation(projects.sync.work)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)
    implementation(libs.androidx.compose.material3.windowSizeClass)
    implementation(libs.androidx.compose.runtime.tracing)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.tracing.ktx)
    implementation(libs.androidx.window.core)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.coil.kt)
    implementation(libs.lottie.compose)

    // Firebase Messaging for push notifications
    implementation(libs.firebase.cloud.messaging)

    // OneSignal SDK for push notification delivery (iOS parity)
    implementation(libs.onesignal)

    // Kotlinx Serialization for JSON parsing
    implementation(libs.kotlinx.serialization.json)

    // Retrofit for HostModule
    implementation(libs.retrofit.core)
    implementation(libs.gson.convert)
    implementation(libs.retrofit.gson)

    // Auth0 SDK for authentication
    implementation(libs.auth0)

    // Timber for logging
    implementation(libs.timber)

    // AndroidX Security Crypto for EncryptedSharedPreferences
    implementation(libs.androidx.security.crypto)

    // OkHttp Logging Interceptor
    implementation(libs.okhttp.logging)

    // Coil Compose for AsyncImage
    implementation(libs.coil.kt.compose)

    // Google Maps (detail map preview)
    implementation(libs.google.play.maps)
    implementation(libs.google.maps.compose)
    // Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // AndroidX Browser for CustomTabs
    implementation(libs.androidx.browser)

    // Stripe Android SDK for Payment Methods
    implementation("com.stripe:stripe-android:22.5.0")

    implementation(libs.identity.credential)
    implementation(project(":feature:home"))
    implementation(project(":feature:home:presentation"))
    implementation(project(":core:database"))

    ksp(libs.hilt.compiler)

    debugImplementation(libs.androidx.compose.ui.testManifest)

    kspTest(libs.hilt.compiler)

    testImplementation(libs.androidx.compose.ui.test)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.hilt.android.testing)



    baselineProfile(projects.benchmarks)
}

baselineProfile {
    // Don't build on every iteration of a full assemble.
    // Instead enable generation directly for the release build variant.
    automaticGenerationDuringBuild = false
}

dependencyGuard {
    configuration("prodReleaseRuntimeClasspath")
}
